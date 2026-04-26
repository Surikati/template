import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { MessageService } from 'primeng/api';

import {
  AdminApiService,
  AppSettingsResponse,
  UpdateAppSettingsRequest,
} from '@tmpmgmt/api-client';
import { ProblemDetail } from '@tmpmgmt/core';

@Component({
  selector: 'tm-app-settings',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DatePipe, ReactiveFormsModule, ButtonModule, InputTextModule],
  template: `
    <div class="page-head">
      <h1>Nastavení aplikace</h1>
    </div>

    @if (loading()) {
      <p class="muted">Načítání…</p>
    } @else {
      <form [formGroup]="form" class="form" (ngSubmit)="save()">
        <p class="hint">
          Výchozí hodnoty pro formátování čísel, dat a měn v generovaných
          dokumentech. Klient může jednotlivé volby přepsat per-request přes
          hlavičky <code>X-Locale</code>, <code>X-Timezone</code>,
          <code>X-Currency</code>.
        </p>

        <label>
          <span>Locale (BCP 47)</span>
          <input
            pInputText
            id="settings-locale"
            formControlName="locale"
            placeholder="cs-CZ"
            autocomplete="off"
          />
          <small class="muted">např. <code>cs-CZ</code>, <code>en-US</code>, <code>sk-SK</code></small>
        </label>

        <label>
          <span>Časová zóna (IANA)</span>
          <input
            pInputText
            id="settings-timezone"
            formControlName="timezone"
            placeholder="Europe/Prague"
            autocomplete="off"
          />
          <small class="muted">např. <code>Europe/Prague</code>, <code>UTC</code></small>
        </label>

        <label>
          <span>Měna (ISO 4217)</span>
          <input
            pInputText
            id="settings-currency"
            formControlName="currency"
            placeholder="CZK"
            maxlength="3"
            autocomplete="off"
            class="currency-input"
          />
          <small class="muted">tříznakový kód, např. <code>CZK</code>, <code>EUR</code>, <code>USD</code></small>
        </label>

        <div class="actions">
          <p-button
            type="submit"
            label="Uložit"
            icon="pi pi-save"
            [loading]="saving()"
            [disabled]="form.invalid || form.pristine"
          />
        </div>

        @if (current(); as s) {
          <p class="audit muted">
            Naposledy upraveno: <strong>{{ s.updatedAt | date: 'medium' }}</strong>
            @if (s.updatedBy) {
              uživatelem <code>{{ s.updatedBy }}</code>
            }
          </p>
        }
      </form>
    }
  `,
  styles: [
    `
      .page-head { margin-bottom: 1rem; }
      h1 { margin: 0; font-size: 1.5rem; }
      .form { max-width: 28rem; display: flex; flex-direction: column; gap: 1.1rem; }
      .form label { display: flex; flex-direction: column; gap: 0.35rem; }
      .form label > span { font-weight: 500; color: #27272a; }
      .form .currency-input { text-transform: uppercase; max-width: 10rem; }
      .form .hint { color: #52525b; font-size: 0.9rem; margin: 0; }
      .form .actions { display: flex; justify-content: flex-end; }
      .form .audit { font-size: 0.85rem; margin: 0; }
      .muted { color: #71717a; }
      code { font-family: ui-monospace, SFMono-Regular, Consolas, monospace; font-size: 0.85em; }
    `,
  ],
})
export class AppSettingsComponent {
  private readonly api = inject(AdminApiService);
  private readonly messages = inject(MessageService);
  private readonly fb = inject(FormBuilder);

  protected readonly loading = signal(true);
  protected readonly saving = signal(false);
  protected readonly current = signal<AppSettingsResponse | null>(null);

  protected readonly form = this.fb.nonNullable.group({
    locale: ['', [Validators.required]],
    timezone: ['', [Validators.required]],
    currency: ['', [Validators.required]],
  });

  constructor() {
    this.api.getSettings().subscribe({
      next: (s) => {
        this.current.set(s);
        this.form.reset({ locale: s.locale, timezone: s.timezone, currency: s.currency });
        this.loading.set(false);
      },
      error: (err: ProblemDetail) => {
        this.loading.set(false);
        this.messages.add({
          severity: 'error',
          summary: 'Nelze načíst nastavení',
          detail: err.detail ?? err.title,
        });
      },
    });
  }

  protected save(): void {
    if (this.form.invalid) return;
    this.saving.set(true);
    const req: UpdateAppSettingsRequest = {
      locale: this.form.controls.locale.value.trim(),
      timezone: this.form.controls.timezone.value.trim(),
      currency: this.form.controls.currency.value.trim().toUpperCase(),
    };
    this.api.updateSettings(req).subscribe({
      next: (s) => {
        this.saving.set(false);
        this.current.set(s);
        this.form.reset({ locale: s.locale, timezone: s.timezone, currency: s.currency });
        this.messages.add({
          severity: 'success',
          summary: 'Uloženo',
          detail: 'Nastavení aplikace bylo aktualizováno.',
        });
      },
      error: (err: ProblemDetail) => {
        this.saving.set(false);
        const detail =
          err.violations && err.violations.length > 0
            ? err.violations.join('; ')
            : (err.detail ?? err.title);
        this.messages.add({ severity: 'error', summary: 'Chyba ukládání', detail });
      },
    });
  }
}
