package i4nc4mp.myLock;

//We need a separate mediator subclass here because we will use a show_when_locked version of the lockscreen
//this way users can have a lockscreen to control music even during pattern lockdown.
//this method is more secure and convenient than disabling secure mode if all they need is basic control /info viewing while in lockdown

//we will not complete the lifecycle till we have a user present broadcast meaning they did actually unlock the device