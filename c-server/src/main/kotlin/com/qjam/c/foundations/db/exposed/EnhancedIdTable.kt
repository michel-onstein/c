/*
 * Copyright 2018 DoorBuzzin, Q-Jam B.V.
 */
package com.qjam.c.foundations.db.exposed

import org.jetbrains.exposed.dao.IdTable

abstract class EnhancedIdTable<T : Comparable<T>> constructor(name: String = "") : IdTable<T>(name) {
}
