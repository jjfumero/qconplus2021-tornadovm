
###################################################################
# Blur Filter
###################################################################

## Sequential 
tornado -cp target/qconplus2021-1.0-SNAPSHOT.jar qconplus2021.samples.BlurFilter --sequential

## Multi-thread with Java Streams
tornado -cp target/qconplus2021-1.0-SNAPSHOT.jar qconplus2021.samples.BlurFilter --mt

# With TornadoVM using the Loop Parallel API
tornado -cp target/qconplus2021-1.0-SNAPSHOT.jar qconplus2021.samples.BlurFilter --tornado 

# With TornadoVM using the Parallel Kernel API
tornado -cp target/qconplus2021-1.0-SNAPSHOT.jar qconplus2021.samples.BlurFilter --tornadoContext


###################################################################
# Julia Sets
###################################################################

tornado  qconplus2021.samples.JuliaSets --seq

tornado  qconplus2021.samples.JuliaSets --mt

## Run Julia Sets with TornadoVM accelerated on GPUs
tornado -cp target/qconplus2021-1.0-SNAPSHOT.jar qconplus2021.samples.JuliaSets --tornado


###################################################################
# DFT
###################################################################

## Run DFT 
## This program has three arguments:
##   <size> 
##   <mode: parallel | sequential > 
##   <iterations> 
tornado -cp target/qconplus2021-1.0-SNAPSHOT.jar qconplus2021.samples.DFT 8192 parallel 100


## Selecting another backend (if installed with TornadoVM)
tornado -Ds0.t0.device=1:0 -cp target/qconplus2021-1.0-SNAPSHOT.jar qconplus2021.samples.DFT 8192 parallel 100


## Print Generated Kernel
tornado --printKernel -cp target/qconplus2021-1.0-SNAPSHOT.jar qconplus2021.samples.DFT 8192 parallel 100


## Display in which accelerator the applications was launched and the block of threads used
tornado --threadInfo -cp target/qconplus2021-1.0-SNAPSHOT.jar qconplus2021.samples.DFT 8192 parallel 100
