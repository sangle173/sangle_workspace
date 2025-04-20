package com.example.local_cloud.controller;

import com.example.local_cloud.service.SpaceService;
import com.example.local_cloud.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;

@Controller
@RequestMapping("/spaces")
public class SpaceController {

    @Autowired
    private SpaceService spaceService;

    @Autowired
    private CategoryService categoryService;

    // Show list of all spaces
    @GetMapping
    public String index(Model model) {
        model.addAttribute("spaces", spaceService.listSpaces());
        return "space_list";
    }

    // Create a new space
    @PostMapping("/create")
    public String createSpace(@RequestParam String name, RedirectAttributes redirectAttributes) throws IOException {
        spaceService.createSpace(name);
        redirectAttributes.addFlashAttribute("toastMessage", "✅ Space '" + name + "' created successfully!");
        redirectAttributes.addFlashAttribute("toastType", "success");
        return "redirect:/spaces";
    }

    // Delete a space
    @PostMapping("/delete")
    public String deleteSpace(@RequestParam String name, RedirectAttributes redirectAttributes) throws IOException {
        spaceService.deleteSpace(name);
        redirectAttributes.addFlashAttribute("toastMessage", "🗑️ Space '" + name + "' deleted.");
        redirectAttributes.addFlashAttribute("toastType", "danger");
        return "redirect:/spaces";
    }

    // List all categories inside a space
    @GetMapping("/{space}/categories")
    public String listCategories(@PathVariable String space, Model model) {
        model.addAttribute("space", space);
        model.addAttribute("categories", categoryService.listCategories(space));
        return "category_list";
    }

    // Redirect a category to note list
    @GetMapping("/{space}/categories/{category}")
    public String redirectToNoteList(@PathVariable String space,
                                     @PathVariable String category) {
        return "redirect:/spaces/" + space + "/categories/" + category + "/notes";
    }

    // Create a new category inside a space
    @PostMapping("/{space}/categories/create")
    public String createCategory(@PathVariable String space,
                                 @RequestParam String name,
                                 RedirectAttributes redirectAttributes) throws Exception {
        categoryService.createCategory(space, name);
        redirectAttributes.addFlashAttribute("toastMessage", "✅ Category '" + name + "' created!");
        redirectAttributes.addFlashAttribute("toastType", "success");
        return "redirect:/spaces/" + space + "/categories";
    }

    // Delete a category inside a space
    @PostMapping("/{space}/categories/delete")
    public String deleteCategory(@PathVariable String space,
                                 @RequestParam String name,
                                 RedirectAttributes redirectAttributes) throws Exception {
        categoryService.deleteCategory(space, name);
        redirectAttributes.addFlashAttribute("toastMessage", "🗑️ Category '" + name + "' deleted.");
        redirectAttributes.addFlashAttribute("toastType", "danger");
        return "redirect:/spaces/" + space + "/categories";
    }

    // Rename space
    @PatchMapping("/rename")
    @ResponseBody
    public String renameSpace(@RequestParam String oldName,
                              @RequestParam String newName) throws IOException {
        spaceService.renameSpace(oldName, newName);
        return "success";
    }

    // Rename category
    @PatchMapping("/{space}/categories/rename")
    @ResponseBody
    public String renameCategory(@PathVariable String space,
                                 @RequestParam String oldName,
                                 @RequestParam String newName) throws IOException {
        categoryService.renameCategory(space, oldName, newName);
        return "success";
    }
}
