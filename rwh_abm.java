package rwh;

import sim.engine.SimState;

public class rwh_abm extends SimState {
    /*Number of hours in month (April) */
    public static int hoursInMonth = 720;

    private static final long serialVersionUID = 1;

    public rwh_abm(long seed) {
        super(seed);
    }

    public void start(String[] arguments) {
        super.start();
        schedule.scheduleRepeating(new market(arguments));
    }

    public static void main(String[] args, int seedIndex) {
        rwh_abm abm = new rwh_abm(seedIndex);
        abm.start(args);
        long steps = 0;
        while (steps < hoursInMonth) //*******************************************************This controls the number of steps in model!!!!!!!!
        {
            if (!abm.schedule.step(abm))
                break;
            steps = abm.schedule.getSteps();
            if (steps % 500 == 0)
                System.out.println("Steps: " + steps + " Time: " + abm.schedule.getTime());
        }
        abm.finish();
    }
}
  
  
