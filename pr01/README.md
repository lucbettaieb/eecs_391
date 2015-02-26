#eecs 391
Luc Bettaieb (lab149) and Aidan Campbell (rac158)
###Assignment 1
####Comments
We used a priority queue (min at top heap) for our open list for optimization purposes.

For replanning, we used a weighted look-ahead and look-behind to see if the bad guy was in either of those locations.  If he was, we replanned.  If he was no longer in our more optimal path, we are less likely to replan.

We also experimented with a vector-based approach to looking at the location of the goal vs. the location of the enemy.  If the vectors from the footman to the goal and the footman to the enemy had the same basis, and the magnitdue of the vector from the footman to the enemy was low, a replan would occur.