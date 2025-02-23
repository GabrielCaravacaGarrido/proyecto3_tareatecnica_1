package com.project.demo.rest.categoryRest;

import com.project.demo.logic.entity.http.GlobalResponseHandler;
import com.project.demo.logic.entity.http.Meta;
import com.project.demo.logic.entity.category.Category;
import com.project.demo.logic.entity.category.CategoryRepo;
import com.project.demo.logic.entity.product.Product;
import com.project.demo.logic.entity.product.ProductRepo;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/category")
public class CategoryRestController {
    @Autowired
    private CategoryRepo categoryRepo;

    @Autowired
    private ProductRepo productRepo;


    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getCategory(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request
    ) {

        Pageable pageable = PageRequest.of(page - 1, size);
        Page<Category> categoriesPage = categoryRepo.findAll(pageable);
        Meta meta = new Meta(request.getMethod(), request.getRequestURL().toString());
        meta.setTotalPages(categoriesPage.getTotalPages());
        meta.setTotalElements(categoriesPage.getTotalElements());
        meta.setPageNumber(categoriesPage.getNumber() + 1);
        meta.setPageSize(categoriesPage.getSize());

        return new GlobalResponseHandler().handleResponse("Categories retrieved successfully",
                categoriesPage.getContent(), HttpStatus.OK, meta);

    }


    @GetMapping("/{Id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getCategoryById(@PathVariable Long Id, HttpServletRequest request) {
        Optional<Category> foundCategory = categoryRepo.findById(Id);
        if (foundCategory.isPresent()) {
            return new GlobalResponseHandler().handleResponse(
                    "Category retrieved successfully",
                    foundCategory.get(),
                    HttpStatus.OK,
                    request);
        } else {
            return new GlobalResponseHandler().handleResponse(
                    "Category with id " + Id + " not found",
                    HttpStatus.NOT_FOUND,
                    request);
        }
    }


    @PostMapping
    @PreAuthorize("isAuthenticated() && hasAnyRole('SUPER_ADMIN')")
    public ResponseEntity<?> addCategory(@RequestBody Category category, HttpServletRequest request) {
        Category savedCategory = categoryRepo.save(category);
        return new GlobalResponseHandler().handleResponse(
                "Category successfully saved",
                savedCategory,
                HttpStatus.OK,
                request);
    }


    @PutMapping
    @PreAuthorize("isAuthenticated() && hasAnyRole('SUPER_ADMIN')")
    public ResponseEntity<?> updateCategory(@RequestBody Category category, HttpServletRequest request) {
        Category savedCategory = categoryRepo.save(category);
        return new GlobalResponseHandler().handleResponse(
                "Category successfully updated",
                savedCategory,
                HttpStatus.OK,
                request);
    }


    @DeleteMapping("/{Id}")
    @PreAuthorize("isAuthenticated() && hasAnyRole('SUPER_ADMIN')")
    public ResponseEntity<?> deleteCategory(@PathVariable Long Id, HttpServletRequest request) {
        Optional<Category> foundCategory = categoryRepo.findById(Id);
        if (foundCategory.isPresent()) {
            categoryRepo.deleteById(Id);
            return new GlobalResponseHandler().handleResponse(
                    "Category successfully deleted",
                    HttpStatus.OK,
                    request);
        } else {
            return new GlobalResponseHandler().handleResponse(
                    "Category with id " + Id + " not found",
                    HttpStatus.NOT_FOUND,
                    request);
        }
    }

    @PatchMapping("/{Id}/addProduct")
    @PreAuthorize("isAuthenticated() && hasAnyRole('SUPER_ADMIN')")
    public ResponseEntity<?> addProductToCategories(
            @PathVariable Long Id,
            @RequestBody Product product,
            HttpServletRequest request) {

        Optional<Category> foundCategory = categoryRepo.findById(Id);
        if (foundCategory.isPresent()) {
            Product toSaveProduct;
            if (product.getId() != null) {
                Optional<Product> foundProduct = productRepo.findById(product.getId());
                if (foundProduct.isPresent()) {
                    toSaveProduct = foundProduct.get();
                } else {
                    toSaveProduct = new Product();
                }
            } else {
                toSaveProduct = new Product();
            }

            toSaveProduct.setname(product.getname());
            toSaveProduct.setdescription(product.getdescription());
            toSaveProduct.setPrice(product.getPrice());
            toSaveProduct.setStock(product.getStock());
            toSaveProduct.setCategory(foundCategory.get());

            if (foundCategory.get().getProducts().contains(toSaveProduct)) {
                return new GlobalResponseHandler().handleResponse(
                        "Product already in this category",
                        HttpStatus.BAD_REQUEST,
                        request);
            }

            productRepo.save(toSaveProduct);
            foundCategory.get().getProducts().add(toSaveProduct);
            categoryRepo.save(foundCategory.get());

            return new GlobalResponseHandler().handleResponse(
                    "Product added to category " + foundCategory.get().getName(),
                    foundCategory.get(),
                    HttpStatus.OK,
                    request);
        } else {
            return new GlobalResponseHandler().handleResponse(
                    "Category with id " + Id + " not found",
                    HttpStatus.NOT_FOUND,
                    request);
        }
    }

    @PatchMapping("/{Id}/removeProduct")
    @PreAuthorize("isAuthenticated() && hasAnyRole('SUPER_ADMIN')")
    public ResponseEntity<?> rmProductToCategory(@PathVariable Long Id, @RequestBody Product product, HttpServletRequest request) {
        Optional<Category> foundCategory = categoryRepo.findById(Id);

        if (foundCategory.isPresent()) {

            if (product.getId() == null) {
                return new GlobalResponseHandler().handleResponse(
                        "Product id is required",
                        HttpStatus.BAD_REQUEST,
                        request);
            }

            Optional<Product> foundProduct = productRepo.findById(product.getId());
            if (!foundProduct.isPresent()) {
                return new GlobalResponseHandler().handleResponse(
                        "Product not found",
                        HttpStatus.NOT_FOUND,
                        request);
            }

            Product toRemoveProduct = foundProduct.get();

            if (!foundCategory.get().getProducts().contains(toRemoveProduct)) {
                return new GlobalResponseHandler().handleResponse(
                        "Product not in this category",
                        HttpStatus.BAD_REQUEST,
                        request);
            }

            foundCategory.get().getProducts().remove(toRemoveProduct);
            categoryRepo.save(foundCategory.get());
            productRepo.delete(toRemoveProduct);

            return new GlobalResponseHandler().handleResponse(
                    "Product removed from the category",
                    foundCategory.get(),
                    HttpStatus.OK,
                    request);
        } else {
            return new GlobalResponseHandler().handleResponse(
                    "Category with id " + Id + " not found",
                    HttpStatus.NOT_FOUND,
                    request);
        }
    }

    @PatchMapping
    @PreAuthorize("isAuthenticated() && hasAnyRole('SUPER_ADMIN')")
    public ResponseEntity<?> updateProduct(@RequestBody Product product, HttpServletRequest request) {

        if (product.getId() == null) {
            return new GlobalResponseHandler().handleResponse(
                    "Product id is required",
                    HttpStatus.BAD_REQUEST,
                    request);
        }

        Optional<Product> foundProduct = productRepo.findById(product.getId());
        if (!foundProduct.isPresent()) {
            return new GlobalResponseHandler().handleResponse(
                    "Product not found",
                    HttpStatus.NOT_FOUND,
                    request);
        }

        Product existingProduct = foundProduct.get();

        if (product.getname() != null) {
            existingProduct.setname(product.getname());
        }
        if (product.getdescription() != null) {
            existingProduct.setdescription(product.getdescription());
        }
        if (product.getPrice() != 0) {
            existingProduct.setPrice(product.getPrice());
        }
        if (product.getStock() != 0) {
            existingProduct.setStock(product.getStock());
        }

        if (product.getCategory() != null && product.getCategory().getId() != null) {
            Optional<Category> foundCategory = categoryRepo.findById(product.getCategory().getId());
            if (foundCategory.isPresent()) {
                existingProduct.setCategory(foundCategory.get());
            } else {
                return new GlobalResponseHandler().handleResponse(
                        "Category not found",
                        HttpStatus.NOT_FOUND,
                        request);
            }
        }

        Product savedProduct = productRepo.save(existingProduct);

        return new GlobalResponseHandler().handleResponse(
                "Product successfully updated",
                savedProduct,
                HttpStatus.OK,
                request);
    }



    /*@PatchMapping("/{categoryId}/updateProduct")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> updateProductInCategory(
            @PathVariable Long categoryId,
            @RequestBody Product product,
            HttpServletRequest request) {

        Optional<Category> foundCategory = categoryRepo.findById(categoryId);
        if (!foundCategory.isPresent()) {
            return new GlobalResponseHandler().handleResponse(
                    "Category with id " + categoryId + " not found",
                    HttpStatus.NOT_FOUND,
                    request);
        }

        if (product.getId() == null) {
            return new GlobalResponseHandler().handleResponse(
                    "Product id is required",
                    HttpStatus.BAD_REQUEST,
                    request);
        }

        Optional<Product> foundProduct = productRepo.findById(product.getId());
        if (!foundProduct.isPresent()) {
            return new GlobalResponseHandler().handleResponse(
                    "Product not found",
                    HttpStatus.NOT_FOUND,
                    request);
        }

        Product existingProduct = foundProduct.get();

        if (!existingProduct.getCategory().getId().equals(categoryId)) {
            return new GlobalResponseHandler().handleResponse(
                    "Product does not belong to this category",
                    HttpStatus.BAD_REQUEST,
                    request);
        }

        if (product.getname() != null) {
            existingProduct.setname(product.getname());
        }
        if (product.getdescription() != null) {
            existingProduct.setdescription(product.getdescription());
        }
        if (product.getPrice() != 0.0) {
            existingProduct.setPrice(product.getPrice());
        }
        if (product.getStock() != 0) {
            existingProduct.setStock(product.getStock());
        }

        Product savedProduct = productRepo.save(existingProduct);

        return new GlobalResponseHandler().handleResponse(
                "Product successfully updated in category " + foundCategory.get().getName(),
                savedProduct,
                HttpStatus.OK,
                request);
    }*/
}