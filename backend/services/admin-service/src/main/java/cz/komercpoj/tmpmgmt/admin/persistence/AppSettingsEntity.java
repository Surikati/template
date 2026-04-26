package cz.komercpoj.tmpmgmt.admin.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Singleton row holding the app-wide locale defaults that VariableFormatter falls back to when a
 * request doesn't supply X-Locale / X-Timezone / X-Currency headers. The DB enforces single-row
 * semantics via a CHECK (id = 1) constraint.
 */
@Entity
@Table(name = "app_settings")
@Getter
@NoArgsConstructor
public class AppSettingsEntity {

  /** The only valid id. Enforced by DB CHECK constraint. */
  public static final short SINGLETON_ID = 1;

  @Id
  @Column(nullable = false)
  private Short id = SINGLETON_ID;

  @Column(nullable = false, length = 20)
  private String locale;

  @Column(nullable = false, length = 50)
  private String timezone;

  @Column(nullable = false, length = 3)
  private String currency;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Column(name = "updated_by")
  private UUID updatedBy;

  public void update(String locale, String timezone, String currency, UUID actor) {
    this.locale = locale;
    this.timezone = timezone;
    this.currency = currency;
    this.updatedAt = Instant.now();
    this.updatedBy = actor;
  }
}
