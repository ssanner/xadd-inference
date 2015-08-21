# Description of AAAI2015 empirical evaluation problems

# Introduction #
Three domains were used in the paper:
1- Reservoir Management
2- Inventory Control
3- Traffic Control

The problems are specified in the .cmdp format which contains:
a list of continuous variables: cvariables ()

The min and max values for each continuous: min-values (), max-values ()

A list of boolean variables: bvariables ()

Action variables: avariables()

A complete description for each action: action act()

containing:

A decision tree transition for each continuous variable: X' ([X+1])

A decision tree of probability of true for every boolean variable: B' ( B: 0.8: 0.7)

The reward another decision tree: (X>0: 10: -10)

# Details #
Problem 1 - RESERVOIR MANAGEMENT (Yeh 1985; Mahootchi 2009). In this problem, the task is to manage a water supply reservoir chain made up of k reservoirs each with a continuous water level Li. For each reservoir there are actions for drain-i and no-drain-i actions determine whether water is drained from reservoir i to the next. The amount of drained water is linear on the water level, and the amount of energy produced is proportional to the product of drained water and water level. At every iteration, the top reservoir receives a constant spring replenishment plus a non-deterministic rain while the bottom reservoir has a part of its water used for city purposes. See file at: https://code.google.com/p/xadd-inference/source/browse/trunk/src/camdp/ex/initialstateNonlinear/reservoir3.cmdp

Problem 2 - INVENTORY CONTROL (Scarf 2002). An inventory control problem, consists of determining what itens from the inventory should be ordered and how much to order of each. There is a continuous variable xi for each item i and a single action order with continuous parameters dxi for each item. There is a fixed limit L for the maximum number of items in the warehouse and a limit li for how much of an item can be ordered at a single stage. The items are sold according to a stochastic demand, modelled by boolean variables di that represent whether demand is high (Q units) or low (q units) for item i. A reward is given for each sold item, but there are linear costs for items ordered and held in the warehouse. See file at: https://code.google.com/p/xadd-inference/source/browse/trunk/src/camdp/ex/initialstateContact/inventory2.cmdp

Problem 3 - Nonlinear TRAFFIC CONTROL (Daganzo 1994). This domain describes a ramp metering problem
using a cell transmission model. Each segment i before the merge is represented by a continuous car density ki and a probabilistic boolean hii indicator of higher incoming car density. The segment c after the merge is represented by its continuous car density kc and speed vc. Each action cor- respond to a traffic light cycle that gives more time to one segment, so that the controller can give priority a segment with a greater density by choosing the action corresponding to it. See file at: https://code.google.com/p/xadd-inference/source/browse/trunk/src/camdp/ex/initialstateNonlinear/traffic1.cmdp