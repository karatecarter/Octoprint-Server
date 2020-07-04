/**
 *  Octoprint Server
 *
 *  Copyright 2020 Daniel Carter
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 */
 // CHANGE LOG:
 // 07/04/2020 - Add capabilities for use in other apps (Motion Sensor = print active; Temperature Management = hotend temp; Relative Humidity Measurement = print progress)
 //              Add option to send push notification when print is complete
 //              Add option to bypass print failure check and another option to use autooff when print fails
 //              Add tile to set dry run mode (printer runs without heating or extruding)
 // 05/02/2020 - Add settings for autooff delay or temperature threshhold; increase polling while printer is on; fix to only display events in feed if status has changed; change port to non-required due to possible bug
 // 03/31/2020 - Format progress with 0 decimal places for display status
 // 03/07/2020 - Initial Release

metadata {
	definition (name: "Octoprint Server", namespace: "karatecarter", author: "Daniel Carter", cstHandler: true) {
		capability "Refresh"
        capability "Switch"
        capability "Motion Sensor" // printing status
        capability "Temperature Measurement" // hotend temp
		capability "Relative Humidity Measurement" // print progress
        
        attribute "displayStatus", "string"
        attribute "printProgress", "number"
        attribute "bedTemp", "number"
        attribute "hotendTemp", "number"
        attribute "bedTempSetpoint", "number"
        attribute "hotendTempSetpoint", "number"
        attribute "printTime", "string"
        attribute "remainingTime", "string"
        attribute "autoOff", "boolean"
        attribute "printComplete", "boolean"
        attribute "currentJobName", "string"
        attribute "currentJobOrigin", "string"
        attribute "dryRunMode", "boolean"
        
        command "heatPla"
        command "heatAbs"
        command "heatOff"
        command "setBedTemp"
        command "setToolTemp"
        command "printSelectedFile"
        command "printLocalFile"
        command "printSdFile"
        command "pause"
        command "resume"
        command "cancel"
        command "dryRunOn"
        command "dryRunOff"
        command "toggleAutoOff"
        command "connect"
        command "disconnect"
	}


	simulator {
		// TODO: define status and reply messages here
	}

	tiles(scale: 2) {
		standardTile("displayStatus", "device.displayStatus", width: 2, height: 2, canChangeBackground: false) {
			state("default", label: '${currentValue}%', action: "pause", icon: "st.Office.office19", backgroundColor:"#00a0dc")
			state("paused", label: 'Paused', action: "resume", nextState: "resuming", icon: "st.Office.office19", backgroundColor:"#00a0dc")
			state("resuming", label: 'Resuming', icon: "st.Office.office19", backgroundColor:"#00a0dc")
			state("offline", label: "Offline", action: "connect", icon: "st.Office.office19", nextState: "connecting", backgroundColor:"#ffffff")
			state("connecting", label: "...", icon: "st.Office.office19", backgroundColor:"#ffffff")
            state("unavailable", label: "Unavailable", icon: "st.Office.office19", backgroundColor:"#ff0e05")
            state("idle", label: "Idle", icon: "st.Office.office19", backgroundColor:"#cccccc")
		}
        valueTile("printTime", "device.printTime", width: 4, height: 1, inactiveLabel: false, decoration: "flat") {
            state "default", label:'Print Time: ${currentValue}'
            state "00:00:00", label:'Print Time: N/A'
        }
        valueTile("remainingTime", "device.remainingTime", width: 4, height: 1, inactiveLabel: false, decoration: "flat") {
            state "default", label:'Remaining: ${currentValue}'
            state "00:00:00", label:'Remaining: N/A'
        }
        
        controlTile("bedTempSlider", "device.bedTempSetpoint", "slider", width: 4, height: 1, inactiveLabel: false, range: "(40..100)") {
            state "default", label:'Bed Setpoint: ${currentValue}', action:"setBedTemp", backgroundColor:"#e86d13"
        }
        valueTile("bedTemp", "device.bedTemp", width: 2, height: 1, inactiveLabel: false, decoration: "flat") {
            state "default", label:'Bed: ${currentValue}°'
        } 
        
        
        controlTile("hotendTempSlider", "device.hotendTempSetpoint", "slider", width: 4, height: 1, inactiveLabel: false, range: "(150..250)") {
            state "default", label:'Tool Setpoint: ${currentValue}', action:"setToolTemp", backgroundColor:"#e86d13"
        }
        valueTile("hotendTemp", "device.hotendTemp", width: 2, height: 1, inactiveLabel: false, decoration: "flat") {
            state "default", label:'Tool: ${currentValue}°'
        }
        
        standardTile("heatPla", "device.heatPla", inactiveLabel: false, width: 2, height: 2) {
			state "default", label: "PLA", action:"heatPla", icon:"st.alarm.temperature.overheat", backgroundColor:"#ff0e05"
		}
        
        standardTile("heatAbs", "device.heatAbs", inactiveLabel: false, width: 2, height: 2) {
			state "default", label: "ABS", action:"heatAbs", icon:"st.alarm.temperature.overheat", backgroundColor:"#ff0e05"
		}
        
        standardTile("heatOff", "device.heatOff", inactiveLabel: false, width: 2, height: 2) {
			state "default", label: "Off", action:"heatOff", icon:"st.alarm.temperature.freeze", backgroundColor:"#cccccc"
		}
        
        standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			state "default", action:"refresh.refresh", icon:"st.secondary.refresh"
		}
        standardTile("switch", "device.switch", width: 2, height: 2, decoration: "flat", inactiveLabel: false) {
			state "on", label: "On", action:"off", icon:"st.switches.switch.on", backgroundColor: "#00a0dc"
            state "off", label: "Off", action:"on", icon:"st.switches.switch.off", backgroundColor: "#cccccc"
		}
        
        standardTile("autoOff", "device.autoOff", width: 2, height: 2, decoration: "flat", inactiveLabel: false) {
			state "true", label: "Auto Off", action:"toggleAutoOff", backgroundColor: "#00a0dc"
            state "false", label: "Auto Off", action:"toggleAutoOff", backgroundColor: "#cccccc"
		}
        
        standardTile("dryRun", "device.dryRunMode", width: 2, height: 2, decoration: "flat", inactiveLabel: false) {
			state "true", label: "Dry Run", action:"dryRunOff", backgroundColor: "#00a0dc"
            state "false", label: "Dry Run", action:"dryRunOn", backgroundColor: "#cccccc"
		}
        
		main "displayStatus"
		details(["displayStatus", "printTime", "remainingTime", "hotendTemp", "hotendTempSlider", "bedTemp", "bedTempSlider", "heatPla", "heatAbs", "heatOff", "switch", "autoOff", "refresh", "dryRun"])
	}
    
    preferences {
		section("Device Settings:") {
			input "server", "string", title:"Hosname or IP Address", description: "Host or IP Address", defaultValue: "192.168.1.187", required: true, displayDuringSetup: true
			input "serverport", "number", title:"Port", description: "Port", defaultValue: "80", required: false, displayDuringSetup: true // removing required due to possible bug where Android app may not recognize 2 chars as a valid value
            input "apikey", "string", title:"API Key", description: "API Key", required: true, displayDuringSetup: true
        }
        section("Auto shutoff:") {
			input "autoOffAfterFailure", "boolean", title: "Auto shutoff when print fails", defaultValue: false, required: true, displayDuringSetup: true
            input name: "autooff_type", type: "enum", title:"Auto off trigger", options: ["Time", "Temperature"], description: "When a print completes, wait for this condition before shutting off power", defaultValue: "Temperature", required: true, displayDuringSetup: true
			input "autooff_delay", "number", title:"If trigger is set to Time, shutoff this many seconds after a print completes", description: "Seconds", defaultValue: "60", required: true, displayDuringSetup: true
            input "autooff_temp", "number", title:"If trigger is set to Temp, shutoff after printer hotend cools to this temperature after a print completes", defaultValue: "50", description: "Degrees", required: true, displayDuringSetup: true
        }
        input "sendPushNotifications", "boolean", title: "Send push notifications when print ends", defaultValue: true, required: true, displayDuringSetup: true
        input "checkForFailure", "boolean", title: "Check if print failed when complete (some plugins may cause false reporting of failures)", defaultValue: true, required: true, displayDuringSetup: true
        input "defaultDebugLevel", "number", title:"Printer debug level when dry run is turned off", defaultValue: "0", displayDuringSetup: true
	}
}

import java.text.DecimalFormat

def installed () {
	log.debug "Device Handler installed"
     state.autoOff = false
     sendEvent(name: "autoOff", value: false, linkText: deviceName, displayed: false, isStateChange: true,)
     updated()
}

def updated () {
	log.debug "Device Handler updated"
    
    state.tryCount = 0
    state.pending_autooff = false
    state.dryRunMode = false
    sendEvent(name: "dryRunMode", value: false, linkText: deviceName, displayed: false, isStateChange: true,)
        
    runEvery1Minute(refresh)
    runIn(2, refresh)
}

// handle commands
def deviceNotification(String str, String descriptionText) {
	def displayDeviceStatus = true
    
    //log.debug descriptionText
    def deviceName = getLinkText(device);
    def printDescription
    
    if (device.currentValue('currentJobOrigin') == "sdcard") {
        printDescription = "SD Card print job "
    } else {
        printDescription = "Octoprint Print Job "
    }

    printDescription = printDescription + "${device.currentValue('currentJobName')} "

    if (device.currentValue('status') == "printing" && str == "idle") {
      def success = true
      if (checkForFailure == "true" && device.currentValue('printProgress') == null) success = false
      log.debug "Sending Print Complete; progress=${device.currentValue('printProgress')}; success=$success; autoOff=${device.currentValue('autoOff')}"
      
      if (success) {
        printDescription += "completed in ${device.currentValue('printTime')}"
      } else {
        printDescription += "failed"
      }
      
      sendEvent(name: "printComplete", value: success, linkText: deviceName, descriptionText: printDescription, isStateChange: true, displayed: true, data: {sendPush: sendPushNotifications})
      sendEvent(name: "motion", value: "inactive", displayed: false)
            
      displayDeviceStatus = false
      
      if ((success || autoOffAfterFailure == "true") && device.currentValue('autoOff') == "true")
      {
        if (autooff_type == "Time")
        {
          log.debug "Shutting down in ${autooff_delay} seconds"
          runIn(autooff_delay, "autoOff")
        }
        else
        {
          log.debug "Auto shutdown when temperature cools to ${autooff_temp}"
          state.pending_autooff = true
        }
      }
    }
    
    if (str == "printing") {
      sendEvent(name: "motion", value: "active", displayed: false)
      
      if (device.currentValue('status') == "paused") {
        descriptionText = "Resuming $printDescription"
      } else {
        descriptionText = "Starting $printDescription"
         state.pending_autooff = false;
      }
    }
    
    //log.debug "Status = $str, descriptionText = $descriptionText"
    if (displayDeviceStatus) {
      if (device.currentValue('status') == str) displayDeviceStatus = false
    }
    
    sendEvent(name: "status", value: str, descriptionText: descriptionText, displayed: displayDeviceStatus)
	if (str != "printing") {
    sendEvent(name: "displayStatus", value: str, linkText: deviceName, displayed: false)
    }
}

void callbackParse(physicalgraph.device.HubResponse hubResponse) {
    //log.trace "Received response"
    //log.debug "Response: ${hubResponse}\nDescription: ${hubResponse.description}\nBody: ${hubResponse.body}"
    
    if (hubResponse.description) {
    	parseResponse(hubResponse.description)
    } else {
        log.error "Received an invalid response"
    }
}

def heatPla()
{
  setToolTemp(200)
  setBedTemp(60)
}


def heatAbs()
{
  setToolTemp(215)
  setBedTemp(80)
}


def heatOff()
{
  setToolTemp(0)
  setBedTemp(0)
}



def setToolTemp(temp)
{
    def command = getPostCommand("/api/printer/tool", "{\"command\": \"target\", \"targets\": {\"tool0\": $temp}}")
    sendHubCommand(command)
    runIn(2, refresh)
}

def setBedTemp(temp)
{
	def command = getPostCommand("/api/printer/bed", "{\"command\": \"target\", \"target\": $temp }")
    sendHubCommand(command)
    runIn(2, refresh)
}

def pause()
{
	def command = getPostCommand("/api/job", "{\"command\": \"pause\", \"action\": \"pause\" }")
    sendHubCommand(command)
    runIn(2, refresh)
}

def resume()
{
	def command = getPostCommand("/api/job", "{\"command\": \"pause\", \"action\": \"resume\" }")
    sendHubCommand(command)
    runIn(2, refresh)
}

def printLocalFile(String filePath)
{
  printFile("local", filePath)
}

def printSdFile(String filePath)
{
  printFile("sdcard", filePath)
}

def printFile(String source, String filePath)
{
	def command = getPostCommand("/api/files/$source/$filePath", "{\"command\": \"select\", \"print\": true }")
    sendHubCommand(command)
    runIn(2, refresh)
}

def printSelectedFile()
{
	def command = getPostCommand("/api/job", "{\"command\": \"start\" }")
    sendHubCommand(command)
    runIn(2, refresh)
}

def cancel()
{
	def command = getPostCommand("/api/job", "{\"command\": \"cancel\" }")
    sendHubCommand(command)
    runIn(2, refresh)
}

def connect() {
  //  log.debug "switch = $switch"

    if (device.currentValue('switch') == "off") {
    log.debug "Device is off; switching on"
      on()
    }
    def command = getPostCommand("/api/connection", '{"command":"connect"}')
    state.connected = false
    sendHubCommand(command)
    sendEvent(name: "displayStatus", value: "connecting", linkText: deviceName, displayed: false)
	runIn(2, refresh)
}

def disconnect() {
    def command = getPostCommand("/api/connection", '{"command":"disconnect"}')
    sendHubCommand(command)
	runIn(2, refresh)
}

// parse events into attributes
def parse(description) {
	log.debug "Execute: parse"
    parseResponse(description)
}

def toggleAutoOff() {
  def deviceName = getLinkText(device);
  
  if (state.autoOff == true) {
    log.debug "Disabling auto off"
    state.autoOff = false;
    sendEvent(name: "autoOff", value: false, linkText: deviceName, displayed: false, isStateChange: true,)
  } else {
    log.debug "Enabling auto off"
    state.autoOff = true;
    sendEvent(name: "autoOff", value: true, linkText: deviceName, displayed: false, isStateChange: true,)
  }
}


def dryRunOn() {
  def deviceName = getLinkText(device);
  
  log.debug "Enabling dry run"
  state.dryRunMode = true;
  sendEvent(name: "dryRunMode", value: true, linkText: deviceName, displayed: false, isStateChange: true,)

  setDryRunMode()
}

def dryRunOff() {
  def deviceName = getLinkText(device);
  
  log.debug "Disabling dry run"
  state.dryRunMode = false;
  sendEvent(name: "dryRunMode", value: false, linkText: deviceName, displayed: false, isStateChange: true,)

  setDryRunMode()
}


def setDryRunMode() {
  def command
  
  if (state.dryRunMode) {
    log.debug "Setting dry run mode on"
    command = getPostCommand("/api/printer/command", '{"command":"M111 S8"}')
  }
  else {
    log.debug "Setting dry run mode off"
    if (defaultDebugLevel == null)
    {
      defaultDebugLevel = 0
    }
    command = getPostCommand("/api/printer/command", "{\"command\":\"M111 S${defaultDebugLevel}\"}")
  }
  sendHubCommand(command)
}

private parseResponse(description)
{
	def msg = parseLanMessage(description)
    //log.trace "HTTP Response"//: ${msg}"
    
    if (msg.status == 200) {
      state.tryCount = 0
      
      //log.debug msg.data
      if (msg.data.job) {
        
        def deviceName = getLinkText(device);
        def descriptionText = "${deviceName} is ${msg.data.state}";
        log.debug descriptionText
        if (msg.data.state == "Printing" || msg.data.state == "Printing from SD") {
          log.debug "${deviceName} print progress is ${msg.data.progress.completion}"
          sendEvent(name: "printProgress", value: msg.data.progress.completion, linkText: deviceName, displayed: false)
          sendEvent(name: "humidity", value: msg.data.progress.completion, linkText: deviceName, displayed: false)
          def printTime = new GregorianCalendar( 0, 0, 0, 0, 0, msg.data.progress.printTime, 0 ).time.format( 'HH:mm:ss' )
          def printTimeLeft = new GregorianCalendar( 0, 0, 0, 0, 0, msg.data.progress.printTimeLeft, 0 ).time.format( 'HH:mm:ss' )
          def progressFormat = new DecimalFormat("#")
          log.debug "Progress = ${msg.data.progress.completion} (${progressFormat.format(msg.data.progress.completion)}%)"

          sendEvent(name: "displayStatus", value: progressFormat.format(msg.data.progress.completion), linkText: deviceName, displayed: false)
          sendEvent(name: "currentJobName", value: msg.data.job.file.name, linkText: deviceName, displayed: false)
          sendEvent(name: "currentJobOrigin", value: msg.data.job.file.origin, linkText: deviceName, displayed: false)
          sendEvent(name: "printTime", value: printTime, linkText: deviceName, displayed: false)
          sendEvent(name: "remainingTime", value: printTimeLeft, linkText: deviceName, displayed: false)
          deviceNotification("printing", descriptionText)
        } else if (msg.data.state == "Pausing" || msg.data.state == "Paused") {
          log.debug "${deviceName} is ${msg.data.state}; print progress is ${msg.data.progress.completion}"
          sendEvent(name: "printProgress", value: msg.data.progress.completion, linkText: deviceName, displayed: false)
          sendEvent(name: "humidity", value: msg.data.progress.completion, linkText: deviceName, displayed: false)
          def printTime = new GregorianCalendar( 0, 0, 0, 0, 0, msg.data.progress.printTime, 0 ).time.format( 'HH:mm:ss' )
          def printTimeLeft = new GregorianCalendar( 0, 0, 0, 0, 0, msg.data.progress.printTimeLeft, 0 ).time.format( 'HH:mm:ss' )
          sendEvent(name: "displayStatus", value: "paused", linkText: deviceName, displayed: false)
          sendEvent(name: "currentJobName", value: msg.data.job.file.name, linkText: deviceName, displayed: false)
          sendEvent(name: "currentJobOrigin", value: msg.data.job.file.origin, linkText: deviceName, displayed: false)
          sendEvent(name: "printTime", value: printTime, linkText: deviceName, displayed: false)
          sendEvent(name: "remainingTime", value: printTimeLeft, linkText: deviceName, displayed: false)
          deviceNotification("paused", descriptionText)
        
        } else if (msg.data.state.startsWith("Offline")) {
          state.connected = false
          descriptionText = "${deviceName} is Offline";
          deviceNotification("offline", descriptionText)
          descriptionText = "${deviceName} print progress is 0";
          sendEvent(name: "printProgress", value: 0, linkText: deviceName, displayed: false)
          sendEvent(name: "humidity", value: 0, linkText: deviceName, displayed: false)
          
        } else if (msg.data.state.startsWith("Error")) {
          descriptionText = "${deviceName} is Offline";
          deviceNotification("offline", descriptionText)
          log.error "Octoprint error: ${msg.data.state}"
        } else if (msg.data.state.startsWith("Detecting")) {
          log.debug msg.data.state
          runIn(2, refresh)
        } else if (msg.data.state.startsWith("Connecting")) {
          log.debug msg.data.state
          state.connected = false
          runIn(2, refresh)
        } else {
          if (!state.connected) {
            setDryRunMode()
          }
          state.connected = true
          
          sendEvent(name: "printProgress", value: msg.data.progress.completion, linkText: deviceName, displayed: false)
          sendEvent(name: "humidity", value: msg.data.progress.completion, linkText: deviceName, displayed: false)

          def printTime = new GregorianCalendar( 0, 0, 0, 0, 0, msg.data.progress.printTime ?: 0, 0 ).time.format( 'HH:mm:ss' )
          def printTimeLeft = new GregorianCalendar( 0, 0, 0, 0, 0, msg.data.progress.printTimeLeft ?: 0, 0 ).time.format( 'HH:mm:ss' )
          sendEvent(name: "currentJobName", value: msg.data.job.file.name, linkText: deviceName, displayed: false)
          sendEvent(name: "currentJobOrigin", value: msg.data.job.file.origin, linkText: deviceName, displayed: false)
          sendEvent(name: "printTime", value: printTime, linkText: deviceName, displayed: false)
          sendEvent(name: "remainingTime", value: printTimeLeft, linkText: deviceName, displayed: false)
          deviceNotification("idle", descriptionText)
        }
      } else if (msg.data.temperature) {
        sendEvent(name: "hotendTemp", value: msg.data.temperature.tool0.actual, displayed: false)
      sendEvent(name: "temperature", value: msg.data.temperature.tool0.actual, unit: "C", displayed: false)
        sendEvent(name: "bedTemp", value: msg.data.temperature.bed.actual, displayed: false)
        // sendEvent(name: "humidity", value: msg.data.temperature.bed.actual, unit: "C", displayed: false)
        
        sendEvent(name: "hotendTempSetpoint", value: msg.data.temperature.tool0.target, displayed: false)
        sendEvent(name: "bedTempSetpoint", value: msg.data.temperature.bed.target, displayed: false)
        
        if (state.pending_autooff && autooff_type == "Temperature")
        {
          log.debug "Autooff pending: curent temperature=${msg.data.temperature.tool0.actual}; autooff temperature=${autooff_temp}"
          if (msg.data.temperature.tool0.actual <= autooff_temp)
          {
            autoOff()
          }
        }
        
        if (device.currentValue('status') != "offline") {
          runIn(15, refresh)
          log.debug "Refresh in 15 seconds"
        } else {
          runEvery1Minute(refresh)
        }
      } else {
        def deviceName = getLinkText(device);
        def descriptionText = "${deviceName} is unavailable";
        if (state.tryCount <= 5) {
          log.debug "Bad response received; retrying"
          runIn(2, refresh)
        } else {
          log.warn "Too many bad responses; marking ${deviceName} as unavailable"
          deviceNotification("unavailable", descriptionText)
          runEvery1Minute(refresh)
        }
      }
    }
}

// handle commands
def refresh() {
	log.debug "Execute: refresh"

	state.tryCount = state.tryCount + 1

//	log.debug "state.tryCount: ${state.tryCount}"
    
    if (state.tryCount > 3) {
//    	log.debug "***** OFFLINE *****"
        log.debug "No log data after ${state.tryCount - 1} refreshes"
        def deviceName = getLinkText(device);
        def descriptionText = "${deviceName} is unavailable";
        log.debug descriptionText
        deviceNotification("unavailable", descriptionText)
    }
    
  	def command = getGetCommand("/api/job")
    sendHubCommand(command)
    
    command = getGetCommand("/api/printer?exclude=state,sd")
    sendHubCommand(command)
}


def getGetCommand(String apiRoute) {
    def hostAddress = getHostAddress()
    
    def result = new physicalgraph.device.HubAction(
        method: "GET",
        path: apiRoute,
        contentType: 'application/json',
        headers: [
            HOST: hostAddress,
            "X-Api-Key": apikey
        ],
                String dni = null,
                [callback: callbackParse]
    )
    
    def ip = ""
    
    log.trace "Sending hubAction command -> GET http://${getHostAddress()}${apiRoute}"
    
    return result
}



def getPostCommand(String apiRoute, body) {
    def hostAddress = getHostAddress()
    
    def result = new physicalgraph.device.HubAction(
        method: "POST",
        path: apiRoute,
        contentType: 'application/json',
        headers: [
            HOST: hostAddress,
            "X-Api-Key": apikey,
            "content-type": "application/json"
        ],
        body: body,
                String dni = null,
                [callback: callbackParse]
    )
    
    def ip = ""
    
    log.debug "Sending hubAction command -> POST http://${getHostAddress()}${apiRoute}: $body"
    
    return result
}


// gets the address of the device
private getHostAddress() {
    def ip = server
    def port = serverport

    if (!ip || !port) {
        log.warn "Server IP not defined for device: ${device.id}"
	}

    return ip + ":" + port
}

def on()
{
  log.debug("Turning on...")
  def deviceName = getLinkText(device);
  def descriptionText = "${deviceName} is on";
  sendEvent(name: "switch", value: "on", linkText: deviceName, displayed: false)
  runIn(4, connect)
}


def off()
{
  log.debug("Turning off...")
  def deviceName = getLinkText(device);
  def descriptionText = "${deviceName} is off";
  sendEvent(name: "switch", value: "off", linkText: deviceName, displayed: false)
  runIn(2, refresh)
}

def setSwitch(boolean switchState)
{
  if (switchState) {
    on()
  } else {
    off()
  }
}

def autoOff()
{
  state.pending_autooff = false
  if (device.currentValue('autoOff') == "true") {
    off()
  } else {
    log.debug "Auto shutdown cancelled"
  }
}