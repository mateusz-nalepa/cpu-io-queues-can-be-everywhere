function smallProcessingStep(taskNumber, label, results) {
    logMessage("### Task number: " + taskNumber + " Processing step: " + label)
    results.push(label);
}


const delaySync = (ms) => {
    const end = Date.now() + ms;
    while (Date.now() < end) {
    } // busy wait
};

export function bigProcessing(taskNumber) {
    logMessage("### Start big function for task number: " + taskNumber)
    const results = [];

    const steps = [
        () => smallProcessingStep(taskNumber, "step 1", results),
        () => smallProcessingStep(taskNumber, "step 2", results),
        () => smallProcessingStep(taskNumber, "step 3", results),
        () => smallProcessingStep(taskNumber, "step 4", results),
        () => smallProcessingStep(taskNumber, "step 5", results),
    ];

    for (const step of steps) {
        step();
        delaySync(1000); // eventLoop is not able to do anything
    }

    logMessage("### Ended big function for task number: " + taskNumber)
    return results;
}


function logMessage(message) {
    const d = new Date();
    console.log(`${d.getHours()}:${d.getMinutes()}:${d.getSeconds()}.${d.getMilliseconds()}   ` + message);
}