import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { Questionnaire } from './model/questionnaire';

@Component({
  selector: 'tmq-questionnaire-runner',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section>
      <h2>{{ questionnaire().name }}</h2>
      <!-- TODO: iterate sections/questions, evaluate visibilityRule via backend, collect answers -->
    </section>
  `,
})
export class QuestionnaireRunnerComponent {
  readonly questionnaire = input.required<Questionnaire>();
  readonly completed = output<Record<string, unknown>>();
}
