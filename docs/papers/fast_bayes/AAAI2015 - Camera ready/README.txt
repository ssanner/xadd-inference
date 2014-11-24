To run the experiments in the “Linear-time Gibbs sampler in Piecewise Graphical Models”
paper, in the XADD-inference project (https://code.google.com/p/xadd-inference/) you
should simply run the following class:

hgm.poly.reports.LinearTimeGibbsSamplingInPiecewiseGMsTester

By default, time (to reach an average error less than 3.0) vs. the number of data points
for the BPPL experiment will be executed where the number of dimensions is fixed to be 15.
To test time vs. dimensions for BPPL experiment (with the number of data points fixed as 12)
the first line (i.e. Test test = Test.BPPL_DIM_TEST) should be modified as follows:

Test test = Test.BPPL_DATA_TEST;

Similarly to test time vs. #data points (with #dims fixed as 10) for MMM experiment:

Test time = MMM_DATA_TEST;

Finally, to test time vs. #dims (with #data = 8) for MMM experiment:

Test time = MMM_DIM_TEST;

To specify the path in which the results are persisted, the following Strings should be set:

REPORT_PATH_FOR_FINAL_BPPL_DIM_ANALYSIS
REPORT_PATH_FOR_FINAL_BPPL_DATA_ANALYSIS
REPORT_PATH_FOR_FINAL_MMM_DIM_ANALYSIS
REPORT_PATH_FOR_FINAL_MMM_DATA_ANALYSIS

By following the program flow it can be seen that modifying other parameters is also
straightforward.

In each report path several files are generated.
The files with names starting with “fixed-data” are associated with the dimension analysis tests.
For example a file named “fixed-data12-full.gibbs.general.bppl” persists the dimension
(first column in the file) vs. time (in nanoseconds) to reach an error rate less than 3.0 (2nd
column in the file) for the Baseline Gibbs (with algorithm alias: full.gibbs.general) for #data being
fixed as 12.
The alias of different algorithms as follows:

“rej.general”                   for 	Rejection sampling
“tunned.mh.general”             for 	Tuned MH
“full.gibbs.general”            for 	Baseline Gibbs
“targeted.gated.gibbs.general”  for 	Augmented Gibbs

To regenerate the graph (b) in Figure 5, the program should be run for 30 iterations (in different
output folders) and mean and average computed.
For data analysis tests, similar data are generated in files with names starting with “fixed-dim”.

