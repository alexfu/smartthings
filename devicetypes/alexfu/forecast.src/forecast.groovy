/**
 *  Forecast
 *
 *  Copyright 2016 Alex Fu
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
metadata {
  definition (name: "Forecast", namespace: "alexfu", author: "Alex Fu") {
    capability "Temperature Measurement"
    capability "Sensor"
    command "refresh"
  }

  preferences {
    input(name: "frequency", type: "enum",
      options: [5, 10, 15, 30, 60, 180]
      title: "Refresh rate in minutes",
      displayDuringSetup: true,
      required: false,
      defaultValue: 60)

    input(name: "forecastApiKey", type: "string",
      title: "Forecast API Key",
      displayDuringSetup: true,
      required: true)
  }

  tiles {
    valueTile("temperature", "device.temperature", width: 2, height: 2) {
      state "temperature", label:'${currentValue}°',
        backgroundColors:[
          [value: 31, color: "#153591"],
          [value: 44, color: "#1e9cbb"],
          [value: 59, color: "#90d2a7"],
          [value: 74, color: "#44b621"],
          [value: 84, color: "#f1d801"],
          [value: 95, color: "#d04e00"],
          [value: 96, color: "#bc2323"]
        ]
    }

    standardTile("refresh", "device.temperature", decoration: "flat") {
      state "default", label: "", action: "refresh", icon:"st.secondary.refresh"
    }

    main(["temperature"])
    details(["temperature", "refresh"])
  }
}

////////////////////////////////////////////////////////////////////////////////
// Lifecycle methods
////////////////////////////////////////////////////////////////////////////////

// parse events into attributes
def parse(String description) {
  log.debug "Parsing '${description}'"
}

def installed() {
  fetchCurrentWeather()
  startSchedule()
}

def uninstalled() {
  unschedule()
}

def updated() {
  unschedule()
  fetchCurrentWeather()
  startSchedule()
}

////////////////////////////////////////////////////////////////////////////////
// Custom commands
////////////////////////////////////////////////////////////////////////////////

def refresh() {
  fetchCurrentWeather()
}

////////////////////////////////////////////////////////////////////////////////
// Custom methods
////////////////////////////////////////////////////////////////////////////////

def startSchedule() {
  switch(frequency) {
    case 5:
      runEvery5Minutes(fetchCurrentWeather)
      break;
    case 10:
      runEvery10Minutes(fetchCurrentWeather)
      break;
    case 15:
      runEvery15Minutes(fetchCurrentWeather)
      break;
    case 30:
      runEvery30Minutes(fetchCurrentWeather)
      break;
    case 60:
      runEvery1Hour(fetchCurrentWeather)
      break;
    case 180:
      runEvery3Hours(fetchCurrentWeather)
      break;
  }
}

def fetchCurrentWeather() {
    def params = [
      uri: "https://api.forecast.io",
      path: "/forecast/$forecastApiKey/$location.latitude,$location.longitude"
    ]
    try {
      log.info "Fetching current weather..."
        httpGet(params) { resp ->
            def units = resp.data.flags.units == "us" ? "F" : "C"
            def temp = resp.data.currently.temperature

            // Send temperature change event
            log.info "Sending temperature event with value = $temp"
            sendEvent(name: "temperature", value: temp, unit: units)

            // Schedule next weather check
            log.info "Next weather check in $frequency minutes"
        }
    } catch(error) {
    	log.error "Error fetching current weather $error"
    }
}
