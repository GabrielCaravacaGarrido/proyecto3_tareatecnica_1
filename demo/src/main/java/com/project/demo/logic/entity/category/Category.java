package com.project.demo.logic.entity.category;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.project.demo.logic.entity.product.Product;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Category")
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)

    private Long id;
    private String name;
    private String description;

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL)
    @JsonManagedReference
    private List<Product> products = new ArrayList<>();

    public Category(Long id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    public Category() {}

    public Long getId() { return id; }

    public String getName() {
        return name;
    }

    public String getdescription() {
        return description;
    }

    public List<Product> getProducts() { return products; }
}
