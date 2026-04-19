import axios from 'axios'

export function setupAxios() {
    axios.defaults.timeout = 30000
}
