package com.wavro.studio.projects

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.wavro.studio.domain.StudioProject

class ProjectRepository(context: Context) {
    private val prefs = context.getSharedPreferences("wavro_projects", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val KEY_CURRENT_PROJECT = "current_project"
        private const val KEY_SAVED_TEMPLATES = "saved_templates"
    }

    // ==========================================
    // 1. SAVE & LOAD CURRENT PROJECT
    // ==========================================
    
    fun saveCurrentProject(project: StudioProject) {
        val json = gson.toJson(project)
        prefs.edit().putString(KEY_CURRENT_PROJECT, json).apply()
    }

    fun loadCurrentProject(): StudioProject? {
        val json = prefs.getString(KEY_CURRENT_PROJECT, null) ?: return null
        return try {
            gson.fromJson(json, StudioProject::class.java)
        } catch (e: Exception) {
            null
        }
    }

    // ==========================================
    // 2. SAVE & LOAD CUSTOM TEMPLATES
    // ==========================================
    
    fun saveTemplate(project: StudioProject) {
        val templates = loadTemplates().toMutableList()
        // Remove existing template with same name if it exists
        templates.removeAll { it.name == project.name }
        templates.add(project)
        
        val json = gson.toJson(templates)
        prefs.edit().putString(KEY_SAVED_TEMPLATES, json).apply()
    }

    fun loadTemplates(): List<StudioProject> {
        val json = prefs.getString(KEY_SAVED_TEMPLATES, null) ?: return emptyList()
        val type = object : TypeToken<List<StudioProject>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun deleteTemplate(projectName: String) {
        val templates = loadTemplates().toMutableList()
        templates.removeAll { it.name == projectName }
        val json = gson.toJson(templates)
        prefs.edit().putString(KEY_SAVED_TEMPLATES, json).apply()
    }

    // ==========================================
    // 3. CLEAR ALL DATA
    // ==========================================
    
    fun clearAll() {
        prefs.edit().clear().apply()
    }
}