/**
 *  Copyright 2015 SmartThings
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
 preferences {
      input("ip", "string", title:"IP Address of Nuki Bridge", description: "Eg: 192.168.1.105", required: false, displayDuringSetup: true)
      input("port", "string", title:"Nuki Bridge Port", description: "Eg: 8080", defaultValue: "8080" , required: false, displayDuringSetup: true)
      input("token", "string", title:"API token", description: "Eg: c8v7b6", required: false, displayDuringSetup: true)
}
metadata {
	definition (name: "Nuki Bridge", namespace: "zak", author: "zak") {
		capability "Lock"
		capability "Battery"
		capability "Polling"
		capability "Refresh"
		capability "Actuator"
		capability "Sensor"
		attribute "triggerswitch", "string"
        attribute "level","string"
        
	}

    simulator {
	// TODO: define status and reply messages here
}

tiles {
	standardTile("toggle", "device.lock", width: 2, height: 2) {
			state "locked", label:'locked', action:"lock.unlock", icon:"st.locks.lock.locked", backgroundColor:"#79b821", nextState:"unlocking"
			state "unlocked", label:'unlocked', action:"lock.lock", icon:"st.locks.lock.unlocked", backgroundColor:"#ff0000", nextState:"locking"
			state "unknown", label:"unknown", action:"lock.lock", icon:"st.locks.lock.unknown", backgroundColor:"#ffffff", nextState:"locking"
			state "locking", label:'locking', icon:"st.locks.lock.locked", backgroundColor:"#79b821"
			state "unlocking", label:'unlocking', icon:"st.locks.lock.unlocked", backgroundColor:"#ffffff"
		}
		standardTile("level", "device.level", width: 1, height: 1, inactiveLabel: false, canChangeIcon: false) {
			state ("default", label:'unknown')
			state ("low", label:'Battery Low', backgroundColor: "#bc2323")
			state ("ok", label:'Battery OK',  backgroundColor: "#79b821")
		}
		standardTile("refresh", "device.lock", inactiveLabel: false, decoration: "flat") {
			state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh"
		}
        main "toggle"
		details(["toggle", "lock", "unlock", "level", "refresh"])
	}
}

// parse events into attributes
def parse(String description) {
 	 log.trace "Parsing '${description}'"
     
     def map
	 def headerString
	 def bodyString
	 def slurper
	 def result
     
     map = stringToMap(description)
	 headerString = new String(map.headers.decodeBase64())
     log.trace "map: $map"
     log.trace "headerString: $headerString"
	 //if (headerString.contains("200 OK")) {
		bodyString = new String(map.body.decodeBase64())
		slurper = new groovy.json.JsonSlurper()
		result = slurper.parseText(bodyString)
     log.trace "$result"
     log.trace "{$result.state}"
     if (result.error != "invalid nukiId"){
     switch (result.state) {
			case "1":
            sendEvent(name: "lock", value: "locked", isStateChange: true)
			log.debug "locked"
			break;
            case "2":
            sendEvent(name: "lock", value: "unlocked", isStateChange: true)
            log.debug "unlocked"
            break;
    }
    switch (result.batteryCritical) {
			case "false":
            sendEvent(name: "level", value: "ok", isStateChange: true)
			log.debug "locked"
			break;
            case "true":
            sendEvent(name: "level", value: "low", isStateChange: true)
            log.debug "unlocked"
            break;
     }
     } else 
     log.debug "Nuki Bridge not setup with Smart Lock"
  //}       
}

// handle commands
def lock() {
  log.debug "Executing lock"
  sendEvent(name: "lock", value: "locked", isStateChange: true)
	doLock()   
}

def unlock() {
	log.debug "Executing unlock"
  sendEvent(name: "lock", value: "unlocked", isStateChange: true)
	doUnlock()
}

def poll() {
	log.debug "Executing 'poll'"
  getLockData()
}

def refresh() {
	log.debug "Executing 'refresh'"
  getLockData()
}

private getLockData() {
  getAction("/lockState?nukiId=1&token=$token")
}

private doLock() {
	getAction("/lockAction?nukiId=1&action=2&token=$token")
}

private doUnlock() {
	getAction("/lockAction?nukiId=1&action=1&token=$token")
}

private getAction(uri){
  setDeviceNetworkId(ip,port)  

  def hubAction = new physicalgraph.device.HubAction(
    method: "GET",
    path: uri,
    headers: getHeader()
  )
  log.debug("Executing hubAction on " + getHostAddress())
  log.debug hubAction
  return hubAction    
}

private getHeader(){
	log.debug "Getting headers"
    def headers = [:]
    headers.put("HOST", getHostAddress())
    return headers
}

private delayAction(long time) {
	new physicalgraph.device.HubAction("delay $time")
}

private setDeviceNetworkId(ip,port){
  	def iphex = convertIPtoHex(ip)
  	def porthex = convertPortToHex(port)
  	device.deviceNetworkId = "$iphex:$porthex"
  	log.debug "Device Network Id set to ${iphex}:${porthex}"
}

private getHostAddress() {
	return "${ip}:${port}"
}

private String convertIPtoHex(ipAddress) { 
    String hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02x', it.toInteger() ) }.join()
    return hex
}

private String convertPortToHex(port) {
	String hexport = port.toString().format( '%04x', port.toInteger() )
  return hexport
}
