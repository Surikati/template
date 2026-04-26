import { TestBed } from '@angular/core/testing';
import { Questionnaire } from './model/questionnaire';
import { QuestionnaireRunnerComponent } from './questionnaire-runner.component';

describe('QuestionnaireRunnerComponent', () => {
  const emptyQuestionnaire: Questionnaire = {
    id: 'q-1',
    templateId: 't-1',
    templateVersionNumber: 1,
    name: 'Smlouva — kontrola',
    sections: [],
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [QuestionnaireRunnerComponent],
    });
  });

  it('renders the questionnaire title in the form heading', () => {
    const fixture = TestBed.createComponent(QuestionnaireRunnerComponent);
    fixture.componentRef.setInput('questionnaire', emptyQuestionnaire);
    fixture.detectChanges();

    const title = fixture.nativeElement.querySelector('.title') as HTMLElement;
    expect(title.textContent?.trim()).toBe('Smlouva — kontrola');
  });

  it('omits sections whose visibility entry is explicitly false', () => {
    const fixture = TestBed.createComponent(QuestionnaireRunnerComponent);
    fixture.componentRef.setInput('questionnaire', {
      ...emptyQuestionnaire,
      sections: [
        { id: 's-1', ordinal: 0, title: 'Vždy viditelná', visibilityRule: null, questions: [] },
        { id: 's-2', ordinal: 1, title: 'Skrytá', visibilityRule: null, questions: [] },
      ],
    });
    fixture.componentRef.setInput('visibility', { 's-2': false });
    fixture.detectChanges();

    const titles = Array.from(
      fixture.nativeElement.querySelectorAll('section h3') as NodeListOf<HTMLElement>,
    ).map((h) => h.textContent?.trim());
    expect(titles).toContain('Vždy viditelná');
    expect(titles).not.toContain('Skrytá');
  });

  it('treats missing visibility entries as visible', () => {
    const fixture = TestBed.createComponent(QuestionnaireRunnerComponent);
    fixture.componentRef.setInput('questionnaire', {
      ...emptyQuestionnaire,
      sections: [
        { id: 's-1', ordinal: 0, title: 'Sekce', visibilityRule: null, questions: [] },
      ],
    });
    fixture.detectChanges();

    const heading = fixture.nativeElement.querySelector('section h3') as HTMLElement;
    expect(heading.textContent?.trim()).toBe('Sekce');
  });
});
