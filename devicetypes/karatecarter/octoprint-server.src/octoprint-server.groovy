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
metadata {
	definition (name: "Octoprint Server", namespace: "karatecarter", author: "Daniel Carter", cstHandler: true) {
		capability "Notification"
        capability "Refresh"
        capability "Switch"
        
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
        
        command "heatPla"
        command "heatAbs"
        command "heatOff"
        command "setBedTemp"
        command "setToolTemp"
        command "print"
        command "pause"
        command "cancel"
        command "dryRunOn"
        command "dryRunOff"
        command "toggleAutoOff"
        command "connect"
	}


	simulator {
		// TODO: define status and reply messages here
	}

	tiles(scale: 2) {
		standardTile("displayStatus", "device.displayStatus", width: 2, height: 2, canChangeBackground: false) {
			state("default", label: '${currentValue}%', icon: "st.Office.office19", backgroundColor:"#00a0dc")
			state("offline", label: "Offline", action: "connect", icon: "st.Office.office19", nextState: "connecting", backgroundColor:"#ff0e05")
			state("connecting", label: "...", icon: "st.Office.office19", backgroundColor:"#ff0e05")
            state("unavailable", label: "Unavailable", icon: "st.Office.office19", backgroundColor:"#ffffff")
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
        
		main "displayStatus"
		details(["displayStatus", "printTime", "remainingTime", "hotendTemp", "hotendTempSlider", "bedTemp", "bedTempSlider", "heatPla", "heatAbs", "heatOff", "switch", "autoOff", "refresh"])
	}
    
    preferences {
		section("Device Settings:") {
			input "server", "string", title:"Octoprint Server", description: "Host or IP Address", defaultValue: "192.168.1.187", required: true, displayDuringSetup: true
			input "serverport", "string", title:"Octoprint Server", description: "Port", defaultValue: "80", required: true, displayDuringSetup: true
            input "apikey", "string", title:"Octoprint Server", description: "API Key", required: true, displayDuringSetup: true
		}
	}
}

def installed () {
	log.debug "Device Handler installed"
    updated()
}

def updated () {
	log.debug "Device Handler updated"
    
    state.tryCount = 0
    
    runEvery1Minute(refresh)
    runIn(2, refresh)
}

// handle commands
def deviceNotification(String str, String descriptionText) {
	log.debug descriptionText
    def deviceName = getLinkText(device);
    
    if (device.currentValue('status') == "printing" && str == "idle") {
      log.debug "Sending Print Complete; autoOff=${device.currentValue('autoOff')}"
      sendEvent(name: "printComplete", value: true, linkText: deviceName, descriptionText: "Print Complete", isStateChange: true, displayed: false)
      if (device.currentValue('autoOff') == "true")
      {
        log.debug "Shutting down in 30 seconds"
        runIn(30, "autoOff")
      }
    }
    
    sendEvent(name: "status", value: str, descriptionText: descriptionText)
	if (str != "printing") {
    sendEvent(name: "displayStatus", value: str, linkText: deviceName, displayed: false)
    }
}

void callbackParse(physicalgraph.device.HubResponse hubResponse) {
    log.trace "Received response"
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

def connect() {
  //  log.debug "switch = $switch"

    if (device.currentValue('switch') == "off") {
    log.debug "Device is off; switching on"
      on()
    }
    def command = getPostCommand("/api/connection", '{"command":"connect"}')
    sendHubCommand(command)
    sendEvent(name: "displayStatus", value: "connecting", linkText: deviceName, displayed: false)
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

private parseResponse(description)
{
	def msg = parseLanMessage(description)
    log.trace "HTTP Response"//: ${msg}"
    
    if (msg.status == 200) {
      state.tryCount = 0
      
      //log.debug msg.data
      if (msg.data.job) {
        
        def deviceName = getLinkText(device);
        def descriptionText = "${deviceName} is ${msg.data.state}";
        log.debug descriptionText
        if (msg.data.state == "Printing" || msg.data.state == "Printing from SD") {
          deviceNotification("printing", descriptionText)
          descriptionText = "${deviceName} print progress is ${msg.data.progress.completion}";
          log.debug descriptionText
          sendEvent(name: "printProgress", value: msg.data.progress.completion, linkText: deviceName, displayed: false)
          def printTime = new GregorianCalendar( 0, 0, 0, 0, 0, msg.data.progress.printTime, 0 ).time.format( 'HH:mm:ss' )
          def printTimeLeft = new GregorianCalendar( 0, 0, 0, 0, 0, msg.data.progress.printTimeLeft, 0 ).time.format( 'HH:mm:ss' )
          sendEvent(name: "displayStatus", value: msg.data.progress.completion, linkText: deviceName, displayed: false)
          sendEvent(name: "printTime", value: printTime, linkText: deviceName, displayed: false)
          sendEvent(name: "remainingTime", value: printTimeLeft, linkText: deviceName, displayed: false)

        } else if (msg.data.state.startsWith("Offline")) {
          descriptionText = "${deviceName} is Offline";
          deviceNotification("offline", descriptionText)
          descriptionText = "${deviceName} print progress is 0";
          sendEvent(name: "printProgress", value: 0, linkText: deviceName, displayed: false)
          
        } else if (msg.data.state.startsWith("Error")) {
          descriptionText = "${deviceName} is Offline";
          deviceNotification("offline", descriptionText)
          log.error "Octoprint error: ${msg.data.state}"
        } else if (msg.data.state.startsWith("Detecting")) {
          log.debug msg.data.state
          runIn(2, refresh)
        } else {
          deviceNotification("idle", descriptionText)
          descriptionText = "${deviceName} print progress is 0";
          sendEvent(name: "printProgress", value: 0, linkText: deviceName, displayed: false)
          def printTime = new GregorianCalendar( 0, 0, 0, 0, 0, msg.data.progress.printTime, 0 ).time.format( 'HH:mm:ss' )
          def printTimeLeft = new GregorianCalendar( 0, 0, 0, 0, 0, msg.data.progress.printTimeLeft, 0 ).time.format( 'HH:mm:ss' )
          sendEvent(name: "printTime", value: printTime, linkText: deviceName, displayed: false)
          sendEvent(name: "remainingTime", value: printTimeLeft, linkText: deviceName, displayed: false)
        }
      } else if (msg.data.temperature) {
        sendEvent(name: "hotendTemp", value: msg.data.temperature.tool0.actual, displayed: false)
        sendEvent(name: "bedTemp", value: msg.data.temperature.bed.actual, displayed: false)
        
        sendEvent(name: "hotendTempSetpoint", value: msg.data.temperature.tool0.target, displayed: false)
        sendEvent(name: "bedTempSetpoint", value: msg.data.temperature.bed.target, displayed: false)
        
      } else {
        def deviceName = getLinkText(device);
        def descriptionText = "${deviceName} is unavailable";
        log.debug "Bad response received; marking ${deviceName} as unavailable"
        deviceNotification("unavailable", descriptionText)
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
  if (device.currentValue('autoOff') == "true") {
    off()
  } else {
    log.debug "Auto shutdown cancelled"
  }
}