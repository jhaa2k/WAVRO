package com.wavro.studio.projects

import android.content.Context
import com.google.gson.Gson
import com.wavro.studio.domain.StudioProject

class ProjectRepository(context: Context) {
    private val prefs = context.getSharedPreferences("wavro_projects", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun save(project: StudioProject) {
        prefs.edit().putString(project.id, gson.toJson(project)).apply()
        prefs.edit().putString("last_id", project.id).apply()
    }

    fun load(id: String): StudioProject? {
        val json = prefs.getString(id, null) ?: return null
        return runCatching { gson.fromJson(json, StudioProject::class.java) }.getOrNull()
    }

    fun loadLast(): StudioProject? {
        val id = prefs.getString("last_id", null) ?: return null
        return load(id)
    }

    fun delete(id: String) {
        prefs.edit().remove(id).apply()
    }

    fun listProjects(): List<StudioProject> =
        prefs.all.keys.filter { it != "last_id" }.mapNotNull { load(it) }
}