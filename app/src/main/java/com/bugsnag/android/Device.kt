package com.bugsnag.android

/**
 * Stateless information set by the notifier about the device on which the event occurred can be
 * found on this class. These values can be accessed and amended if necessary.
 */
open class Device internal constructor(
    buildInfo: DeviceBuildInfo,

    /**
     * The Application Binary Interface used
     */
    var cpuAbi: Array<String>?,

    /**
     * Whether the device has been jailbroken
     */
    var jailbroken: Boolean?,

    /**
     * A UUID generated by Bugsnag and used for the individual application on a device
     */
    var id: String?,

    /**
     * The IETF language tag of the locale used
     */
    var locale: String?,

    /**
     * The total number of bytes of memory on the device
     */
    var totalMemory: Long?,

    /**
     * A collection of names and their versions of the primary languages, frameworks or
     * runtimes that the application is running on
     */
    var runtimeVersions: MutableMap<String, Any>?
) : JsonStream.Streamable {

    /**
     * The manufacturer of the device used
     */
    var manufacturer: String? = buildInfo.manufacturer

    /**
     * The model name of the device used
     */
    var model: String? = buildInfo.model

    /**
     * The name of the operating system running on the device used
     */
    var osName: String? = "android"

    /**
     * The version of the operating system running on the device used
     */
    var osVersion: String? = buildInfo.osVersion

    internal open fun serializeFields(writer: JsonStream) {
        writer.name("cpuAbi").value(cpuAbi)
        writer.name("jailbroken").value(jailbroken)
        writer.name("id").value(id)
        writer.name("locale").value(locale)
        writer.name("manufacturer").value(manufacturer)
        writer.name("model").value(model)
        writer.name("osName").value(osName)
        writer.name("osVersion").value(osVersion)
        writer.name("runtimeVersions").value(runtimeVersions)
        writer.name("totalMemory").value(totalMemory)
    }

    override fun toStream(writer: JsonStream) {
        writer.beginObject()
        serializeFields(writer)
        writer.endObject()
    }
}
