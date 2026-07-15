package com.example.browser.data.repository

import com.example.browser.data.dao.TabDao
import com.example.browser.data.entity.TabEntity
import kotlinx.coroutines.flow.Flow

class TabRepository(private val tabDao: TabDao) {

    fun getAllTabs(): Flow<List<TabEntity>> = tabDao.getAll()

    suspend fun saveTabs(tabs: List<TabEntity>) {
        tabDao.deleteAll()
        tabs.forEachIndexed { index, tab ->
            tabDao.insert(tab.copy(position = index))
        }
    }

    suspend fun insertTab(tab: TabEntity): Long = tabDao.insert(tab)

    suspend fun deleteTab(id: Long) = tabDao.deleteById(id)

    suspend fun deleteAll() = tabDao.deleteAll()
}
