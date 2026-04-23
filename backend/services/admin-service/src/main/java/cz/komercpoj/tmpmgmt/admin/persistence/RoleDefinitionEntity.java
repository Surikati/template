package cz.komercpoj.tmpmgmt.admin.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "role_definition")
@Getter
@Setter
@NoArgsConstructor
public class RoleDefinitionEntity {

    @Id
    @Column(length = 50)
    private String code;

    @Column(name = "display_name", nullable = false, length = 200)
    private String displayName;

    @Column(columnDefinition = "TEXT")
    private String description;
}
