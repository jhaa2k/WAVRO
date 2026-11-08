package com.wavro.studio.projects

import android.content.Context
import android.util.Log
import com.wavro.studio.domain.ElementType
import com.wavro.studio.domain.ProjectElement
import com.wavro.studio.domain.StudioProject
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class ProjectRepository(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    fun all(): List<StudioProject> {
        val raw = preferences.getString(PROJECTS_KEY, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            List(array.length()) { index -> decode(array.getJSONObject(index)) }
        } catch (error: JSONException) {
            Log.e(TAG, "Saved projects could not be read", error)
            emptyList()
        } catch (error: IllegalArgumentException) {
            Log.e(TAG, "Saved project contains an unknown element type", error)
            emptyList()
        }
    }

    fun current(): StudioProject {
        val projects = all()
        val currentId = preferences.getString(CURRENT_KEY, null)
        return projects.firstOrNull { it.id == currentId }
            ?: projects.firstOrNull()
            ?: StudioProject()
    }

    fun save(project: StudioProject) {
        val projects = all().filterNot { it.id == project.id } + project
        preferences.edit()
            .putString(PROJECTS_KEY, JSONArray().apply {
                projects.forEach { put(encode(it)) }
            }.toString())
            .putString(CURRENT_KEY, project.id)
            .apply()
    }

    fun select(project: StudioProject) {
        preferences.edit().putString(CURRENT_KEY, project.id).apply()
    }

    fun delete(projectId: String) {
        val remaining = all().filterNot { it.id == projectId }
        preferences.edit()
            .putString(PROJECTS_KEY, JSONArray().apply {
                remaining.forEach { put(encode(it)) }
            }.toString())
            .apply()
    }

    private fun encode(project: StudioProject) = JSONObject().apply {
        put("id", project.id)
        put("name", project.name)
        put("backgroundColor", project.backgroundColor)
        put("elements", JSONArray().apply {
            project.elements.forEach { element ->
                put(JSONObject().apply {
                    put("id", element.id)
                    put("type", element.type.name)
                    put("enabled", element.enabled)
                })
            }
        })
    }

    private fun decode(json: JSONObject): StudioProject {
        val elements = json.optJSONArray("elements") ?: JSONArray()
        return StudioProject(
            id = json.getString("id"),
            name = json.optString("name", "Untitled Project"),
            backgroundColor = json.optLong("backgroundColor", 0xFF0D0D12),
            elements = List(elements.length()) { index ->
                val item = elements.getJSONObject(index)
                ProjectElement(
                    id = item.optString("id"),
                    type = ElementType.valueOf(item.getString("type")),
                    enabled = item.optBoolean("enabled", true),
                )
            },
        )
    }

    private companion object {
        const val TAG = "ProjectRepository"
        const val PREFERENCES = "wavro_projects"
        const val PROJECTS_KEY = "projects"
        const val CURRENT_KEY = "current_project"
    }
}
