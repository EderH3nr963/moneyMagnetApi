package com.moneyMagnetApi.demo.domain.category;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "pluggy_categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PluggyCategory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "pluggy_category_id", nullable = false, unique = true, length = 120)
    private String pluggyCategoryId;
    
    @Column(nullable = false, length = 160)
    private String description;
    
    @Column(name = "description_translated", length = 160)
    private String descriptionTranslated;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private PluggyCategory parent;

    @OneToMany(mappedBy = "parent")
    @Builder.Default
    private List<PluggyCategory> children = new ArrayList<>();

    @OneToMany(mappedBy = "pluggyCategory")
    @Builder.Default
    private List<PluggyCategoryMapping> mappings = new ArrayList<>();
}
