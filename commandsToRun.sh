

## Run Julia Sets with TornadoVM accelerated on GPUs
tornado -Dversion=tornado -cp target/qconplus2021-1.0-SNAPSHOT.jar qconplus2021.samples.JuliaSets


## Run DFT 
## This program has three arguments:
##   <size> 
##   <mode: parallel | sequential > 
##   <iterations> 
tornado -Dversion=tornado -cp target/qconplus2021-1.0-SNAPSHOT.jar qconplus2021.samples.DFT 8192 parallel 100


## Selecting another backend (if installed with TornadoVM)
tornado -Ds0.t0.device=1:0 -Dversion=tornado -cp target/qconplus2021-1.0-SNAPSHOT.jar qconplus2021.samples.DFT 8192 parallel 100


## Print Generated Kernel
tornado --printKernel -Dversion=tornado -cp target/qconplus2021-1.0-SNAPSHOT.jar qconplus2021.samples.DFT 8192 parallel 100


## Display in which accelerator the applications was launched and the block of threads used
tornado --threadInfo -Dversion=tornado -cp target/qconplus2021-1.0-SNAPSHOT.jar qconplus2021.samples.DFT 8192 parallel 100
