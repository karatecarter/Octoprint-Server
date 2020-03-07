/**
*  iCamera Connect and Motion Monitor
*
*  Copyright 2017 Daniel Carter
*
*  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License. You may obtain a copy of the License at:
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
*  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
*  for the specific language governing permissions and limitations under the License.
*
*/
 // CHANGE LOG:
 // 03/03/2020 - Initial Release

definition(
    name: "Octoprint Device Manager",
    namespace: "karatecarter",
    author: "Daniel Carter",
    description: "Octoprint Server SmartApp",
    //category: "Office",
    iconUrl: "http://cdn.device-icons.smartthings.com/Office/office19-icn.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Office/office19-icn@2x.png",
    iconX3Url: "http://cdn.device-icons.smartthings.com/Office/office19-icn@3x.png")


preferences {
    page(name: "configurePrinters", title: "Octoprint Servers")
    page(name: "printerSettings", title: "Printer Settings")
    page(name: "removePrinterPage", title: "Remove Printer")
    page(name: "confirmedRemovePrinterPage", title: "Remove Printer")
}


private getMaxPrinters() { 5 }

def configurePrinters() {
  if (!state.initialized)
  {
    log.debug "Initializing"
    def id = "-new"
    settings."displayName${id}" = null
  settings."ip${id}" = null
  settings."port${id}" = null
  settings."apiKey${id}" = null
  }
  
  dynamicPage(name: "configurePrinters", title: "Octoprint Servers", install: true, uninstall: true) {
    section("Installed Servers") {
      printers.each { printer ->
        log.debug "Listing ${printer.displayName} (${printer.server})"
        def hrefParams = [request: "printerSettings", isNew: false, printer: printer]
        href(name: "printer-${printer.id}", params: hrefParams, title: printer.label ?: printer.name, page: "printerSettings", description: "", required: false)
      }
    }

    section("New Server")
    {
      def hrefParams = [request: "printerSettings", isNew: true, printer: null]
      href(name: "newprinter", params: hrefParams, title: settings."displayName-new" ?: settings."ip-new" ?: "+ New Printer", page: "printerSettings", description: "", required: false)
    }
  }
}

def printerSettings(params) {
  def printer
  def id
  def title
  def deleted = false
  
  if (params.request == "printerSettings") { // may be used if user hits "back" on next page
    atomicState.params = params
  } else
  {
    log.debug "Retrieving atomicState params"
    params = atomicState.params
  }
  
  log.trace "Printer settings for ${params}"
  
  
  if (params.printer) {
    def dev = printers.find { params.printer.id == it.id }
    if (!dev) { deleted = true }
    
    id = "-${params.printer.id}"
    title = params.printer.label ?: params.printer.name + " Settings"
  } else {
    title = "New Printer"
    id = "-new"
      if (!state.initialized)
  {
    log.debug "Initializing"
    settings."displayName${id}" = null
  settings."ip${id}" = null
  settings."port${id}" = null
  settings."apiKey${id}" = null
  state.initialized = true
  }
  }

  dynamicPage(name: "printerSettings", title: title, uninstall: false, install: false) {
    if (!deleted) {
    section("Printer Settings") {
      if (params.isNew) {input "displayName${id}", "text", title:"Name", description: "Leave blank to use Hostname", defaultValue: printer ? printer.displayName : "", required: false
      input "ip${id}", "text", title:"IP Address/Hostname", description: "Server IP address or hostname", defaultValue: printer ? printer.server : "", required: true
      input "port${id}", "number", title:"Port", description: "Server Port", defaultValue: printer ? printer.serverport : 80, required: true
      input "apiKey${id}", "text", title:"Server API Key", description: "See Octoprint Settings", required: true
      } else {
      paragraph "Go to device settings to change connection parameters"
      }
      if (!params.isNew)
      {
      input "powerSwitch${id}", "capability.switch", title: "Power switch", required: false, multiple:false //, submitOnChange: true
      input "extraSwitches${id}", "capability.switch", title: "Extra power switch(es)", description: "Extra devices to power on/off with printer", required: false, multiple:true //, submitOnChange: true
      } else {
      paragraph "Save this device configuration then return here to set power switched"
      }
    }
    
    if (!params.isNew)
    {
      section("Remove Printer")
      {
        def hrefParams = [request: "removePrinterPage", confirmed: false, printer: params.printer]
        href(name: "removePrinter", params: hrefParams, title: "Click here to remove this printer", page: "removePrinterPage")
      }
    }
    } else {
      section() {
        paragraph "Printer has been deleted"
      }
    }
  }
}

def removePrinterPage(params)
{
  log.debug "Remove Printer Page: $params"
  
  def nextPage
  def name = params.printer.label ?: params.printer.name
  def deleted = false
  
  if (params.request == "removePrinterPage") {
  def dev = printers.find { params.printer.id == it.id }
  if (!dev) { deleted = true }
  
  if (params.confirmed) {
    nextPage = "configurePrinters"
    //removePrinter(params.printer)
    deleted = true
  } else {
    nextPage = ""
  }
  } else {
    deleted = true
    nextPage = "configurePrinters"
  }
  
  dynamicPage(name: "removePrinterPage", title: "Remove Printer", nextPage: nextPage, uninstall: false, install: false) {
    if (!deleted) {
      section() {
        if (params.confirmed) {
          paragraph "Printer $name has been removed."
        } else {
          def hrefParams = [confirmed: true, printer: params.printer]
          href(name: "removePrinter", params: hrefParams, title: "Click here to confirm removal of printer $name", page: "confirmedRemovePrinterPage")
        }
      }
    } else {
      section() {
        paragraph "Printer has been deleted"
      }
    }
  }
}

def confirmedRemovePrinterPage(params)
{
  log.debug "Confirmed remove Printer Page: $params"
  
  def name = params.printer.label ?: params.printer.name
  def deleted = false
  
  
  def dev = printers.find { params.printer.id == it.id }
  if (!dev) { deleted = true }
  
  if (params.confirmed) {
    removePrinter(params.printer)
    deleted = true
  }
  
  
  dynamicPage(name: "confirmedRemovePrinterPage", title: "Remove Printer", nextPage: "configurePrinters", uninstall: false, install: false) {
    if (!deleted) {
      section() {
          paragraph "Printer $name has been removed."
        }
      } else {
      section() {
        paragraph "Printer has been deleted"
      }
    }
  }
}

private buildNewPrinter() {
  def printer = [:]
  def id = "-new"
  printer.displayName = settings."displayName${id}"
  printer.server = settings."ip${id}"
  printer.serverport = settings."port${id}"
  printer.apikey = settings."apiKey${id}"
  def newId = "${printer.server}:${printer.serverport}"
  //state.switches << settings."powerSwitch${id}"
  //state.switches << settings."extraSwitches${id}"
  //log.debug state.switches
  //printer.powerSwitch = settings."powerSwitch${id}".id
  state.initialized = false
  def dev = [dni: newId, prefs: printer, name: printer.displayName ?: printer.ip]

  return dev
}

private getPrinters() {
    def devs = getChildDevices()
    //log.trace "Installed printers found: $devs"
    return devs
}

def installed() {
    state.switches = []
    
    log.debug "Installed"

    initialize()
}

def updated() {
    log.debug "Updated"

    initialize()
}

def initialize() {
    unsubscribe()

    installPrinters()
    
    subscribe(printers, "status", printerStatusChanged)
    subscribe(printers, "switch", printerSwitch)
    
    def powerMap = []
    
    printers.each { printer ->
      subscribe(settings."powerSwitch-${printer.id}", "switch", printerPowerSwitch)
      powerMap << [printer: printer.id, switch: settings."powerSwitch-${printer.id}".id]
    }
    state.powerMap = powerMap
    
    runEvery1Minute("refreshPowerSwitches") // for switches that don't automatically report status
}

def installPrinters()
{
    if (settings."ip-new" && settings."port-new" && "settings.apiKey-new") {
      def installedPrinters = printers
      def dev = buildNewPrinter()
      def existingDev = installedPrinters.find { dev.dni.toLowerCase() == it.deviceNetworkId.toLowerCase() }
      if (!existingDev) {
        try {
          def name = dev.name
	  log.trace "Adding $name"
       def physicalHubs = location.hubs.findAll { it.type == physicalgraph.device.HubType.PHYSICAL } // Ignore Virtual hubs
    def hub = physicalHubs[0]
   
          def childDevice = addChildDevice("karatecarter", "Octoprint Server", dev.dni, hub.id, [label: dev.name, preferences: dev.prefs])
        } catch (e) {
          log.error "Error creating device: ${e}"
        }
      } else {
        log.warn "Device ${existingDev.name} already exists with id ${dev.dni}"
      }
      runIn(1, "deleteNewSettings");
  }
}

def removePrinter(printer) {
  def dev = printers.find { printer.id == it.id }
  deleteChildDevice(dev.deviceNetworkId)
}

private removeChildDevices(delete) {
    delete.each {
        deleteChildDevice(it.deviceNetworkId)
    }
}

private deleteNewSettings() {
    def map = []
    def num = "-new"
    map << "displayName${num}"
    map << "ip${num}"
    map << "port${num}"
    map << "apiKey${num}"
    
    deleteSettings(map)
}

def deleteSettings(map)
{
  log.trace "Deleting settings"
    def mapValues = [:]
    map.each { mapValues[it] = '' }
    app.updateSettings(mapValues)
    map.each { name -> 
        settings[name] = '' 
    }
}

def printerStatusChanged(event)
{
  log.trace "Printer status changed for ${event.displayName}: $event"

    def dev = printers.find { event.deviceId == it.id }
}

def printerSwitch(event)
{
  if (event.value == "on") {
    log.debug "Switching on"
    settings."powerSwitch-${event.deviceId}".on()
  } else {
    log.debug "Switching off"
    settings."powerSwitch-${event.deviceId}".off()
  }
}

def printerPowerSwitch(event)
{
  def powerMap = state.powerMap.find { event.deviceId == it.switch }
  def printer = printers.find { powerMap.printer = it.id }
  
  if (event.value == "on") {
    log.debug "Switching Printer on"
    printer.on()
  } else {
    log.debug "Switching Printer off"
    printer.off()
  }
}

def refreshPowerSwitches() {
  log.trace "Refreshing switches"
  
  state.powerMap.each { map ->
    settings."powerSwitch-${map.printer}".refresh()
  }
}