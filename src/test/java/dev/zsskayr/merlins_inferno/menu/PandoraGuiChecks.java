package dev.zsskayr.merlins_inferno.menu;

import dev.zsskayr.merlins_inferno.client.PandoraAnimation;

import static dev.zsskayr.merlins_inferno.menu.PandoraRecipeRules.*;

/** Runs without a game instance: recipe permutations, quantities, gating and GUI geometry. */
public final class PandoraGuiChecks {
    private static int assertions;
    private static void check(boolean condition, String message) {
        assertions++;
        if (!condition) throw new AssertionError(message);
    }
    private static Recipe match(boolean opened, Ingredient catalyst, Ingredient a, Ingredient b, Ingredient c, int x, int y, int z) {
        return PandoraRecipeRules.match(opened, catalyst, new Ingredient[]{a,b,c}, new int[]{x,y,z});
    }
    public static void main(String[] args) {
        Ingredient[] awakening = {Ingredient.BOOK, Ingredient.EVE, Ingredient.FLAME};
        Ingredient[] oblivion = {Ingredient.OTHERWORLD, Ingredient.INFERNAL, Ingredient.CELESTIAL};
        Ingredient[] purgatory = {Ingredient.NETHERITE, Ingredient.INFERNAL, Ingredient.OTHERWORLD};
        for (int a=0; a<3; a++) for (int b=0; b<3; b++) for (int c=0; c<3; c++) {
            boolean unique = a != b && b != c && a != c;
            check(match(false,Ingredient.STAR,awakening[a],awakening[b],awakening[c],1,1,1)
                    == (unique ? Recipe.AWAKENING : Recipe.NONE), "Awakening is unordered and rejects duplicates");
            check(match(true,Ingredient.VOID,oblivion[a],oblivion[b],oblivion[c],64,16,20)
                    == (unique ? Recipe.OBLIVION : Recipe.NONE), "Oblivion accepts surplus but no duplicates");
            check(match(true,Ingredient.BOOK,purgatory[a],purgatory[b],purgatory[c],3,16,64)
                    == (unique && a == 0 ? Recipe.PURGATORY : Recipe.NONE), "Purgatory counts follow the slot's item");
        }
        check(match(true,Ingredient.STAR,Ingredient.BOOK,Ingredient.EVE,Ingredient.FLAME,1,1,1)==Recipe.NONE,"No repeated awakening");
        check(match(false,Ingredient.VOID,Ingredient.OTHERWORLD,Ingredient.INFERNAL,Ingredient.CELESTIAL,16,16,16)==Recipe.NONE,"Oblivion locked before circle 2");
        check(match(true,Ingredient.EMPTY,Ingredient.OTHERWORLD,Ingredient.INFERNAL,Ingredient.CELESTIAL,16,16,16)==Recipe.NONE,"Catalyst required");
        check(match(true,Ingredient.VOID,Ingredient.OTHERWORLD,Ingredient.INFERNAL,Ingredient.CELESTIAL,16,0,16)==Recipe.NONE,"Every offering required");
        check(match(true,Ingredient.VOID,Ingredient.OTHERWORLD,Ingredient.INFERNAL,Ingredient.CELESTIAL,16,15,16)==Recipe.NONE,"Oblivion needs 16 of each essence");
        check(match(true,Ingredient.VOID,Ingredient.OTHERWORLD,Ingredient.INFERNAL,Ingredient.CELESTIAL,16,16,16)==Recipe.OBLIVION,"Oblivion with exactly 16 each");
        check(match(true,Ingredient.BOOK,Ingredient.NETHERITE,Ingredient.INFERNAL,Ingredient.OTHERWORLD,3,16,16)==Recipe.PURGATORY,"Purgatory recipe");
        check(match(true,Ingredient.BOOK,Ingredient.OTHERWORLD,Ingredient.NETHERITE,Ingredient.INFERNAL,16,64,20)==Recipe.PURGATORY,"Purgatory is unordered and takes surplus");
        check(match(true,Ingredient.BOOK,Ingredient.NETHERITE,Ingredient.INFERNAL,Ingredient.OTHERWORLD,2,16,16)==Recipe.NONE,"Purgatory needs 3 netherite");
        check(match(true,Ingredient.BOOK,Ingredient.NETHERITE,Ingredient.INFERNAL,Ingredient.OTHERWORLD,3,15,16)==Recipe.NONE,"Purgatory needs 16 infernal");
        check(match(true,Ingredient.BOOK,Ingredient.NETHERITE,Ingredient.INFERNAL,Ingredient.OTHER,3,16,16)==Recipe.NONE,"Extra unrelated items reject recipe");
        check(match(false,Ingredient.BOOK,Ingredient.NETHERITE,Ingredient.INFERNAL,Ingredient.OTHERWORLD,3,16,16)==Recipe.NONE,"Purgatory locked before circle 2");
        check(required(Recipe.PURGATORY,Ingredient.NETHERITE)==3 && required(Recipe.PURGATORY,Ingredient.INFERNAL)==16 && required(Recipe.AWAKENING,Ingredient.BOOK)==1,"Consumed quantities");
        for(int degree=0;degree<360;degree++) {
            double angle=Math.toRadians(degree);
            for(int i=0;i<3;i++) {
                int x=PandoraLayout.slotX(i,angle),y=PandoraLayout.slotY(i,angle);
                check(x>=54 && x+16<178 && y>=24 && y+16<132,"Orbit remains inside pause region");
                check(Math.abs(x-108)>=16 || Math.abs(y-70)>=16,"Idle offerings do not overlap catalyst");
                for(int j=i+1;j<3;j++) check(Math.abs(x-PandoraLayout.slotX(j,angle))>=18 || Math.abs(y-PandoraLayout.slotY(j,angle))>=18,"No overlapping orbital slots");
            }
        }
        for(int[] dimensions:new int[][]{{320,240},{480,270},{854,480},{1920,1080}}) {
            float scale=PandoraLayout.scale(dimensions[0],dimensions[1]);
            check(PandoraLayout.WIDTH*scale<=dimensions[0]-8 && PandoraLayout.HEIGHT*scale<=dimensions[1]-8,"Panel fits screen");
            for(int p=0;p<232;p++) {
                double screen=dimensions[0]/2.0+(p-dimensions[0]/2.0)*scale;
                check(Math.abs(PandoraLayout.unscale(screen,dimensions[0],scale)-p)<0.001,"Pointer inverse matches rendering");
            }
        }
        check(PandoraLayout.INVENTORY_X+8*18+16<232,"Nine inventory columns fit");
        check(PandoraLayout.INVENTORY_Y+2*18+16<PandoraLayout.HOTBAR_Y,"Hotbar separate from inventory");
        check(PandoraLayout.HOTBAR_Y+16<256,"Hotbar fits panel");
        check(PandoraLayout.CENTER_Y + PandoraLayout.ORBIT_RADIUS + 9 < PandoraLayout.BUTTON_Y, "Orbit clears forge button");
        check(PandoraLayout.BUTTON_Y + PandoraLayout.BUTTON_HEIGHT < PandoraLayout.INVENTORY_Y - 1, "Button clears inventory");
        check(!PandoraLayout.offeringHovered(108, 70, 116, 78, true), "Central result takes click priority during surplus recovery");
        check(PandoraLayout.offeringHovered(108, 70, 116, 78, false), "No output priority when center has no result");
        check(PandoraLayout.offeringHovered(148.25F, 70.5F, 156.5, 78.25, true), "Returning offering remains clickable outside center");
        check(!PandoraLayout.offeringHovered(148.25F, 70.5F, 100, 78.25, true), "Hit test matches subpixel item position");
        animationChecks();
        System.out.println("Pandora GUI: " + assertions + " assertions passed.");
    }

    private static void animationChecks() {
        PandoraAnimation animation = new PandoraAnimation();
        animation.update(0, false, 0, 0, true, false);
        double angle = animation.angle();
        animation.update(16, false, 0, 0, true, false);
        check(animation.angle() == angle, "Hover pauses idle orbit");
        float previous = 0;
        int movingFrames = 0, fractionalPositions = 0;
        for (int time = 32; time <= 2880; time += 8) {
            float serverProgress = Math.min(59, (time - 32) / 50 + 1) / 60.0F;
            animation.update(time, true, serverProgress, 0, true, false);
            check(animation.progress() <= serverProgress + 0.000001F, "No progress prediction beyond server");
            check(animation.progress() >= previous, "Interpolated progress never reverses");
            check(animation.progress() - previous < 0.01F, "Frame progress has no tick-sized jumps");
            if (animation.progress() > previous) movingFrames++;
            if (Math.abs(animation.slotX(0) - Math.round(animation.slotX(0))) > 0.001) fractionalPositions++;
            previous = animation.progress();
        }
        check(movingFrames > 250, "Animation updates between the server's 20 Hz snapshots");
        check(fractionalPositions > 200, "Item positions retain subpixels");
        check(Math.abs(animation.angle() - angle) > 0.1, "Forging rotates even when hovered");
        check(animation.radius() < 0.01, "Offerings meet at the catalyst before completion");
        animation.update(3000, false, 0, 1, true, false);
        check(animation.flash() == 1, "Completion starts a flash");
        check(animation.radius() == 0, "Completion does not snap surplus items outward");
        animation.update(3300, false, 0, 1, true, false);
        check(animation.radius() > 0 && animation.radius() < PandoraLayout.ORBIT_RADIUS, "Surplus returns gradually");
        animation.update(3700, false, 0, 1, true, false);
        check(animation.flash() == 0 && animation.radius() == PandoraLayout.ORBIT_RADIUS, "Effect settles after completion");
        animation.update(3800, true, 0.25F, 1, true, false);
        animation.update(3900, true, 0.25F, 1, true, false);
        animation.update(4500, true, 0.25F, 1, true, false);
        check(animation.progress() == 0.25F, "A stalled server cannot visually finish a ritual");
        check(animation.flash() == 0, "New craft clears the previous flash");
        animation.update(4600, false, 0, 1, true, true);
        check(animation.angle() == PandoraLayout.INITIAL_ANGLE, "Idle touchscreen keeps vanilla snapback coordinates");
    }
}
