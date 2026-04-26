import { ComponentFixture, TestBed } from '@angular/core/testing';
import { PageHeaderComponent } from './page-header.component';

describe('PageHeaderComponent', () => {
  let fixture: ComponentFixture<PageHeaderComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PageHeaderComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(PageHeaderComponent);
    fixture.componentRef.setInput('title', 'Templates');
  });

  it('renders the title', () => {
    fixture.detectChanges();
    const h1 = fixture.nativeElement.querySelector('h1') as HTMLElement;
    expect(h1.textContent?.trim()).toBe('Templates');
  });

  it('omits the subtitle paragraph when no subtitle is provided', () => {
    fixture.detectChanges();
    const subtitle = fixture.nativeElement.querySelector('p.subtitle');
    expect(subtitle).toBeNull();
  });

  it('renders the subtitle paragraph when a subtitle is provided', () => {
    fixture.componentRef.setInput('subtitle', 'Manage and version your documents');
    fixture.detectChanges();
    const subtitle = fixture.nativeElement.querySelector('p.subtitle') as HTMLElement;
    expect(subtitle).not.toBeNull();
    expect(subtitle.textContent?.trim()).toBe('Manage and version your documents');
  });
});
