import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { TemplateApiService, TemplateResponse } from '@tmpmgmt/api-client';
import { MessageService } from 'primeng/api';
import { Observable, of } from 'rxjs';
import { TemplateListComponent } from './template-list.component';
import { provideNoopAnimations } from '@angular/platform-browser/animations';

describe('TemplateListComponent', () => {
  let fixture: ComponentFixture<TemplateListComponent>;
  let component: TemplateListComponent;
  let listSpy: jasmine.Spy<() => Observable<TemplateResponse[]>>;
  let navigateSpy: jasmine.Spy;
  let messageAddSpy: jasmine.Spy;

  function buildRows(): TemplateResponse[] {
    return [
      {
        id: '11111111-1111-1111-1111-111111111111',
        slug: 'nda',
        name: 'NDA',
        description: 'Mlčenlivost',
        category: 'legal',
        tags: [],
        status: 'ACTIVE',
        ownerUserId: '00000000-0000-0000-0000-000000000000',
        createdAt: '2026-04-01T10:00:00Z',
        updatedAt: '2026-04-20T11:00:00Z',
      } as TemplateResponse,
      {
        id: '22222222-2222-2222-2222-222222222222',
        slug: 'contract',
        name: 'Smlouva',
        description: undefined,
        category: undefined,
        tags: [],
        status: 'ARCHIVED',
        ownerUserId: '00000000-0000-0000-0000-000000000000',
        createdAt: '2026-03-01T10:00:00Z',
        updatedAt: '2026-03-10T11:00:00Z',
      } as TemplateResponse,
    ];
  }

  function setup(rows: TemplateResponse[]): void {
    listSpy = jasmine.createSpy('list').and.returnValue(of(rows));
    navigateSpy = jasmine.createSpy('navigate').and.returnValue(Promise.resolve(true));
    messageAddSpy = jasmine.createSpy('add');

    TestBed.configureTestingModule({
      imports: [TemplateListComponent],
      providers: [
        provideNoopAnimations(),
        provideRouter([]),
        { provide: TemplateApiService, useValue: { list: listSpy } },
        { provide: MessageService, useValue: { add: messageAddSpy } },
      ],
    });
    fixture = TestBed.createComponent(TemplateListComponent);
    component = fixture.componentInstance;
    navigateSpy = spyOn(TestBed.inject(Router), 'navigate').and.returnValue(
      Promise.resolve(true),
    );
    fixture.detectChanges();
  }

  it('calls TemplateApiService.list on initialisation', () => {
    setup([]);
    expect(listSpy).toHaveBeenCalledTimes(1);
  });

  it('renders one row per template returned by the API', () => {
    setup(buildRows());
    const links = fixture.nativeElement.querySelectorAll('tbody a');
    const titles = Array.from(links as NodeListOf<HTMLAnchorElement>).map((a) =>
      a.textContent?.trim(),
    );
    expect(titles).toEqual(['NDA', 'Smlouva']);
  });

  it('shows the empty-state message when no templates are returned', () => {
    setup([]);
    const empty = fixture.nativeElement.querySelector('tbody td.empty') as HTMLElement;
    expect(empty?.textContent?.trim()).toBe('Žádné šablony — vytvořte první.');
  });

  it('onCreated shows a success toast and navigates to the editor', () => {
    setup([]);
    const created: TemplateResponse = {
      id: 'new-id',
      slug: 'fresh',
      name: 'Fresh',
      description: undefined,
      category: undefined,
      tags: [],
      status: 'ACTIVE',
      ownerUserId: '00000000-0000-0000-0000-000000000000',
      createdAt: '2026-04-26T12:00:00Z',
      updatedAt: '2026-04-26T12:00:00Z',
    } as TemplateResponse;

    (component as unknown as { onCreated: (c: TemplateResponse) => void }).onCreated(created);

    expect(messageAddSpy).toHaveBeenCalledWith(
      jasmine.objectContaining({
        severity: 'success',
        summary: 'Šablona vytvořena',
        detail: 'Fresh',
      }),
    );
    expect(navigateSpy).toHaveBeenCalledWith(['/templates', 'new-id', 'edit']);
  });
});
