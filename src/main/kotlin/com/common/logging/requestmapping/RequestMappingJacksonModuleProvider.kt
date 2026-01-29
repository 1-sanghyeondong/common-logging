package com.common.logging.requestmapping

import com.fasterxml.jackson.databind.Module

interface RequestMappingJacksonModuleProvider {
    fun getModules(): List<Module>
}
