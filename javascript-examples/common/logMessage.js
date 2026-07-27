export function logMessage(message) {
    const d = new Date();
    console.log(`${d.getHours()}:${d.getMinutes()}:${d.getSeconds()}.${d.getMilliseconds()}   ` + message);
}