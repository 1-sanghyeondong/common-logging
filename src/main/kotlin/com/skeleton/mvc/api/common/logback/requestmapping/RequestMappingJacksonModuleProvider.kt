package com.skeleton.mvc.api.common.logback.requestmapping

import com.fasterxml.jackson.databind.Module

interface RequestMappingJacksonModuleProvider {
    fun getModules(): List<Module>
}
