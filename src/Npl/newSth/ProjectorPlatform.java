package Npl.newSth;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import Npl.content.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.*;

import static mindustry.Vars.*;

public class ProjectorPlatform extends Block {
    public UnitType ProUnit = CuteUnitTypes.SmallMoonLight;
    public float BuildingTime = 20*60f;
    public float polyStroke = 1.8f, polyRadius = 8f;
    public int polySides = 6;
    public float polyRotateSpeed = 1f;
    public Color polyColor = Pal.accent;
    public ProjectorPlatform(String name){
        super(name);
        solid = true;
        update = true;
        hasItems = true;
        itemCapacity = 200;
        ambientSound = Sounds.loopUnitBuilding;
    }
    @Override
    public boolean outputsItems(){
        return false;
    }

    @Override
    public void setBars(){
        super.setBars();
        addBar("units", (Building e) ->
                new Bar(
                        () ->
                                Core.bundle.format("bar.unitcap",
                                        Fonts.getUnicodeStr(ProUnit.name),
                                        e.team.data().countType(ProUnit),
                                        ProUnit.useUnitCap ? Units.getStringCap(e.team) : "∞"
                                ),
                        () -> Pal.shadow,
                        () -> ProUnit.useUnitCap ? (float)e.team.data().countType(ProUnit) / Units.getCap(e.team) : 1f
                ));
    }
    @Override
    public boolean canPlaceOn(Tile tile, Team team, int rotation){
        return super.canPlaceOn(tile, team, rotation) && Units.canCreate(team, ProUnit);
    }
    public class ProjectorPlatformBuild extends Building implements UnitTetherBlock{
        //needs to be "unboxed" after reading, since units are read after buildings.
        public int readUnitId = -1;
        public float buildProgress, totalProgress;
        public float warmup, readyness;
        public @Nullable Unit unit;
        @Override
        public void updateTile(){
            //unit was lost/destroyed
            if(unit != null && (unit.dead || !unit.isAdded())){
                unit = null;
            }

            if(readUnitId != -1){
                unit = Groups.unit.getByID(readUnitId);
                if(unit != null || !net.client()){
                    readUnitId = -1;
                }
            }
            warmup = Mathf.approachDelta(warmup, efficiency, 1f / 60f);
            readyness = Mathf.approachDelta(readyness, unit != null ? 1f : 0f, 1f / 60f);
            if(unit == null && Units.canCreate(team, ProUnit)){
                buildProgress += edelta() / BuildingTime;
                totalProgress += edelta();

                if(buildProgress >= 1f){
                    if(!net.client()){
                        unit = ProUnit.create(team);
                        if(unit instanceof BuildingTetherc bt){
                            bt.building(this);
                        }
                        unit.set(x, y);
                        unit.rotation = 90f;
                        unit.add();
                        Call.unitTetherBlockSpawned(tile, unit.id);
                    }
                }
            }
        }

        public void spawned(int id){
            Fx.spawn.at(x, y);
            buildProgress = 0f;
            if(net.client()){
                readUnitId = id;
            }
        }

        @Override
        public boolean shouldConsume(){
            return unit == null;
        }

        @Override
        public void draw(){
            Draw.rect(block.region, x, y);
            if(unit == null){
                Draw.draw(Layer.blockOver, () -> {
                    //TODO make sure it looks proper
                    Drawf.construct(this, ProUnit.fullIcon, 0f, buildProgress, warmup, totalProgress);
                });
            }else{
                Draw.z(Layer.bullet - 0.01f);
                Draw.color(polyColor);
                Lines.stroke(polyStroke * readyness);
                Lines.poly(x, y, polySides, polyRadius, Time.time * polyRotateSpeed);
                Draw.reset();
                Draw.z(Layer.block);
            }
        }

        @Override
        public float totalProgress(){
            return totalProgress;
        }

        @Override
        public float progress(){
            return buildProgress;
        }

        @Override
        public void write(Writes write){
            super.write(write);

            write.i(unit == null ? -1 : unit.id);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);

            readUnitId = read.i();
        }
    }
}
