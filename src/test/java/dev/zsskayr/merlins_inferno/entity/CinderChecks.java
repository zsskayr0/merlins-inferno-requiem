package dev.zsskayr.merlins_inferno.entity;

import dev.zsskayr.merlins_inferno.entity.cinder.CinderVitals;

/** Behavioural invariants that do not need a running game. */
public final class CinderChecks {
    private static int assertions;
    private static void check(boolean value, String message) { assertions++; if (!value) throw new AssertionError(message); }
    public static void main(String[] args) {
        CinderVitals v = new CinderVitals();
        for (int i=0;i<59;i++) { v.tick(1,true); check(!v.shouldHeal(), "No healing before three seconds fully under lava"); }
        v.tick(1,true); check(v.shouldHeal(), "Healing starts after uninterrupted submersion");
        int heals=0;
        for (int i=0;i<2400;i++) { v.tick(1,true); if(v.shouldHeal()) heals++; }
        check(heals==120,"Long submersion heals once per second, never every tick after counter saturation");
        v.tick(.99,false);check(!v.shouldHeal() && v.soakedTicks()==0,"Any exposed part resets the healing delay");
        for (int i=0;i<300;i++)v.tick(0,false);
        int cold=v.coldTicks();v.tick(.03,false);check(v.coldTicks()==cold+1,"A tail tip does not reset heat");
        v.tick(.5,false);check(v.coldTicks()==cold-3,"A substantial return to lava gradually reheats");
        check(!v.shouldHeal(),"Partial lava contact cannot regenerate health");
        v.restore(399,0);v.tick(0,false);check(v.shouldFreezeDamage(),"Exposure damage starts at twenty seconds");
        v.restore(599,0);v.tick(0,false);check(v.isTerminal(),"Thirty seconds of exposure is terminal");
        check(Math.abs(v.damageMultiplier()-.3)<.0001,"Cold land attacks are weaker");
        v.restore(-50,4000);check(v.coldTicks()==0 && v.soakedTicks()==1200,"Invalid saved clocks are clamped");
        v.restore(580,0);v.tick(1,true);check(v.coldTicks()==576 && !v.isTerminal(),"A living serpent may reheat before the deadline");
        System.out.println("Cinder: "+assertions+" heat, healing and damage checks passed.");
    }
}
