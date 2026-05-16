import {bigProcessing} from "./processingDefault/processingDefault.js";
import {limitedBigProcessing} from "./processingSEDA/processingSEDA.js";

console.log("############### Default");

await Promise.all([
    bigProcessing(1),
    bigProcessing(2)
]);

console.log();
console.log();
console.log();
console.log();
console.log();
console.log("############### SEDA");

await Promise.all([
    limitedBigProcessing(1),
    limitedBigProcessing(2)
]);
