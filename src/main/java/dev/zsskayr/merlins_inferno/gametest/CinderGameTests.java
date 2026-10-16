package dev.zsskayr.merlins_inferno.gametest;

import dev.zsskayr.merlins_inferno.entity.CinderEntity;
import dev.zsskayr.merlins_inferno.registry.ModEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("merlins_inferno")
@PrefixGameTestTemplate(false)
public final class CinderGameTests {
    @GameTest(template="cinder_empty",timeoutTicks=160)
    public static void groundedPoseDoesNotRestartRise(GameTestHelper h) {
        var c=spawn(h,false);
        c.setNoAi(false);c.setNoGravity(false);
        for(int x=1;x<=36;x++)for(int z=1;z<=36;z++)h.getLevel().setBlock(h.absolutePos(new BlockPos(x,0,z)),Blocks.BASALT.defaultBlockState(),2);
        h.runAtTickTime(80,()-> {
            h.assertTrue(c.onLand() && c.onGround(),"Ground state stays stable after landing");
            h.assertTrue(c.action()==dev.zsskayr.merlins_inferno.entity.cinder.CinderAction.LAND_IDLE,"Rise completes and remains in cobra idle");
        });
        h.runAtTickTime(130,()-> {
            h.assertTrue(c.onLand() && c.action()==dev.zsskayr.merlins_inferno.entity.cinder.CinderAction.LAND_IDLE,"Rise does not restart while stationary");
            h.assertTrue(c.coldTicks()>100,"No reachable lava does not stop cooling");h.succeed();
        });
    }
    @GameTest(template="cinder_empty",timeoutTicks=180)
    public static void landReturnIsSlowAndDoesNotResetCooling(GameTestHelper h) {
        var c=spawn(h,false);c.setNoAi(false);c.setNoGravity(false);
        for(int x=1;x<=36;x++)for(int z=1;z<=36;z++)h.getLevel().setBlock(h.absolutePos(new BlockPos(x,0,z)),Blocks.BASALT.defaultBlockState(),2);
        for(int x=22;x<=28;x++)for(int z=26;z<=34;z++)h.getLevel().setBlock(h.absolutePos(new BlockPos(x,0,z)),Blocks.LAVA.defaultBlockState(),2);
        double start=c.getX();
        h.runAtTickTime(150,()-> {
            h.assertTrue(c.getX()>start+.3,"Stranded Cinder attempts to crawl toward nearby lava");
            h.assertTrue(c.getX()<start+4,"Return is a slow attempt, not a dash or teleport");
            h.assertTrue(c.coldTicks()>100 && c.getHealth()<=60,"Crawling does not stop the cold clock or grant healing");
            h.assertTrue(c.action()!=dev.zsskayr.merlins_inferno.entity.cinder.CinderAction.RISE,"Crawling does not restart rise");h.succeed();
        });
    }
    private static CinderEntity spawn(GameTestHelper h, boolean lava) {
        var level = h.getLevel();
        if (lava) for (int x=1;x<=36;x++) for (int z=1;z<=36;z++) for (int y=1;y<=5;y++)
            level.setBlock(h.absolutePos(new BlockPos(x,y,z)),Blocks.LAVA.defaultBlockState(),2);
        CinderEntity c = ModEntityTypes.CINDER.get().create(level);
        h.assertTrue(c != null,"Cinder registered");
        c.setNoAi(true);c.setNoGravity(true);
        BlockPos pos=h.absolutePos(new BlockPos(18,2,30));
        c.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5,0,0);
        level.addFreshEntity(c);
        return c;
    }

    @GameTest(template="cinder_empty",timeoutTicks=100)
    public static void fullSubmersionHealsOnlyAfterDelay(GameTestHelper h) {
        CinderEntity c=spawn(h,true);c.setHealth(30);
        h.runAtTickTime(45,()-> {
            h.assertTrue(c.fullySubmerged(),"Entire animated body fits under lava");
            h.assertTrue(c.getHealth()==30,"No early healing");
        });
        h.runAtTickTime(85,()-> {
            h.assertTrue(c.getHealth()>=36 && c.getHealth()<=39,"Three HP per second after full submersion delay");
            h.succeed();
        });
    }

    @GameTest(template="cinder_empty",timeoutTicks=100)
    public static void exposedTailStopsRegeneration(GameTestHelper h) {
        CinderEntity c=spawn(h,true);c.setHealth(30);
        h.runAtTickTime(4,()-> {
            var b=c.getParts()[c.getParts().length-1].getBoundingBox();
            BlockPos exposed=BlockPos.containing(b.minX+.05,b.maxY-.05,b.minZ+.05);
            // Keep the whole last few metres open so the swimming sway cannot re-cover it.
            for (int x=-2;x<=2;x++) for (int z=-3;z<=3;z++)
                h.getLevel().setBlock(exposed.offset(x,0,z),Blocks.GLASS.defaultBlockState(),2);
        });
        h.runAtTickTime(80,()-> {
            h.assertTrue(!c.fullySubmerged(),"One exposed body section prevents full submersion");
            h.assertTrue(c.getHealth()==30,"A mostly submerged serpent cannot heal");h.succeed();
        });
    }

    @GameTest(template="cinder_empty",timeoutTicks=40)
    public static void coldSurvivesSaveAndKills(GameTestHelper h) {
        CinderEntity c=spawn(h,false);
        CompoundTag saved=new CompoundTag();c.saveWithoutId(saved);saved.putInt("CinderCold",580);
        c.readAdditionalSaveData(saved);
        h.assertTrue(c.coldTicks()==580,"Cold clock survives loading");
        h.assertTrue(c.damageMultiplier()<.35,"Cold reduces attack strength");
        h.runAtTickTime(25,()-> {h.assertTrue(!c.isAlive(),"Thirty seconds of exposure kills permanently");h.succeed();});
    }

    @GameTest(template="cinder_empty",timeoutTicks=90)
    public static void deadCinderNeverRegenerates(GameTestHelper h) {
        CinderEntity c=spawn(h,true);
        h.runAtTickTime(4,()-> c.hurt(c.damageSources().genericKill(),Float.MAX_VALUE));
        h.runAtTickTime(75,()-> {
            h.assertTrue(!c.isAlive() && c.isRemoved() && c.getHealth()==0,"Lava does not revive a dead Cinder");h.succeed();
        });
    }

    @GameTest(template="cinder_empty",timeoutTicks=15)
    public static void multipartDamageAndSpawnGate(GameTestHelper h) {
        CinderEntity c=spawn(h,false);
        h.runAtTickTime(3,()-> {
            h.assertTrue(c.getParts().length==32,"Every animated body section has a hurtbox");
            for(int i=0;i<c.getParts().length;i++) h.assertTrue(c.getParts()[i].getId()==c.getId()+i+1,"Part identifiers follow the parent");
            for(var part:c.getParts()) {
                h.assertTrue(h.getLevel().getEntityOrPart(part.getId())==part,"Multipart hurtbox is registered in the server entity lookup");
                c.invulnerableTime=0;c.setHealth(60);
                part.hurt(c.damageSources().generic(),2);
                h.assertTrue(c.getHealth()<60,"Every body part forwards damage to the parent");
            }
            c.invulnerableTime=0;c.setHealth(60);
            float old=c.getHealth();
            c.getParts()[20].hurt(c.damageSources().generic(),8);
            h.assertTrue(c.getHealth()<old,"Hitting the tail damages the shared health pool");
            h.assertTrue(!CinderEntity.checkCinderSpawnRules(ModEntityTypes.CINDER.get(),h.getLevel(),MobSpawnType.NATURAL,c.blockPosition(),h.getLevel().random),"Natural Cinder does not spawn in Overworld");
            h.assertTrue(CinderEntity.checkCinderSpawnRules(ModEntityTypes.CINDER.get(),h.getLevel(),MobSpawnType.COMMAND,c.blockPosition(),h.getLevel().random),"Commands bypass habitat restrictions");
            h.succeed();
        });
    }
}
