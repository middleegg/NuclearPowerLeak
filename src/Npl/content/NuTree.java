package Npl.content;

import arc.*;
import arc.func.*;
import arc.scene.style.*;
import arc.struct.*;
import arc.util.*;
import mindustry.ctype.*;
import mindustry.game.Objectives.*;
import mindustry.type.*;
import mindustry.*;
import mindustry.content.*;
import Npl.content.*;
import static mindustry.Vars.*;
import static mindustry.content.TechTree.*;
import static Npl.content.NuBlocks.*;
import static Npl.content.NuItems.*;
import static Npl.content.NuLiquid.*;
import static Npl.content.FederalUnitTypes.*;

public class NuTree{
    public static void load(){
        Azer.Azer.techTree = nodeRoot("Azer",FederalJuniorCore,()->{
            //planets,sector
            node(Azer.Azer);
            //units
            node(ExperimentalMachineryUnitFactory,()->{
               node(ExperimentalUnitReconstructionFactory,()->{
                   node(MechanicalAssemblyFactory,()->{
                   });
                   node(AirshipAssemblyFactory,()->{
                   });
                   node(ShipAssemblyFactory,()->{
                   });
                   node(ParadoxUnitAssemblyFactory,()->{
                      node(TerminalUnitAssemblyFactory,()->{
                      });
                      node(TerminalAssemblyModule,()->{
                      });
                   });
                   node(ParadoxAssemblyModule,()->{
                   });
                   node(NuclearAssemblyParts,()->{
                   });
                   node(AbsurdAssemblyParts,()->{
                   });
                   node(ThalliumAssemblyParts,()->{
                   });
                   node(AssemblyPlantModule,()->{
                   });
                   node(BuildingConstructor,()->{
                   });
               });
               node(bigIronUnitConveyor,()->{
                  node(floatUnitConvryor,()->{

                  });
                  node(floatUnitRouter,()->{

                  });
               });
               node(SpecialUnitFactory,()->{
               });
               node(vile,()->{
                   node(shame,Seq.with(new Research(ExperimentalUnitReconstructionFactory)),()->{
                      node(loss,Seq.with(new Research(AirshipAssemblyFactory)),()->{
                         node(nonsense,Seq.with(new Research(AirshipAssemblyFactory),
                                 new Research(AssemblyPlantModule)),()->{
                            node(cowardTraitor,Seq.with(new Research(ParadoxUnitAssemblyFactory)),()->{
                               node(desperate,Seq.with(new Research(TerminalUnitAssemblyFactory)),()->{

                               });
                            });
                         });
                      });
                   });
               });
               node(honor,()->{
                   node(proud,Seq.with(new Research(ExperimentalUnitReconstructionFactory)),()->{
                      node(vanity,Seq.with(new Research(MechanicalAssemblyFactory)),()->{
                         node(overPraise,Seq.with(new Research(MechanicalAssemblyFactory),
                                 new Research(AssemblyPlantModule)),()->{
                            node(blindLoyalty,Seq.with(new Research(ParadoxUnitAssemblyFactory)),()->{
                               node(safeguardRights,Seq.with(new Research(TerminalUnitAssemblyFactory)),()->{
                               });
                            });
                         });
                      });
                   });
               });
               node(sailor,()->{
                   node(cruise,Seq.with(new Research(ExperimentalUnitReconstructionFactory)),()->{
                      node(wanderer,Seq.with(new Research(ShipAssemblyFactory)),()->{
                         node(setsails,Seq.with(new Research(ShipAssemblyFactory)
                         ,new Research(AssemblyPlantModule)),()->{
                            node(captain,Seq.with(new Research(ParadoxUnitAssemblyFactory)),()->{
                               node(nemo,Seq.with(new Research(TerminalUnitAssemblyFactory)),()->{

                               });
                            });
                         });
                      });
                   });
               });
               node(pale,Seq.with(new Research(PaleUnitFactory)),()->{
                   node(ripple,Seq.with(new Research(PaleNumberReconstruction)),()->{
                      node(greatPath,Seq.with(new Research(PaleMultiplyReconstruction)),()->{
                         node(loyalRequest,Seq.with(new Research(PaleExponentReconstruction)),()->{
                            node(paladin,Seq.with(new Research(PaleImmeasurableReconstruction)),()->{

                            });
                         });
                      });
                   });
               });
               node(mornLight,Seq.with(new Research(PaleUnitFactory)),()->{
                   node(sunsetGlow,Seq.with(new Research(PaleNumberReconstruction)),()->{
                      node(dusk,Seq.with(new Research(PaleMultiplyReconstruction)),()->{
                          node(swallowingDay,Seq.with(new Research(PaleExponentReconstruction)),()->{
                             node(moonLight,Seq.with(new Research(PaleImmeasurableReconstruction)),()->{

                             });
                          });
                      });
                   });
               });
               node(pureJade,Seq.with(new Research(PaleUnitFactory)),()->{
                   node(darkMaple,Seq.with(new Research(PaleNumberReconstruction)),()->{
                      node(brightCrow,Seq.with(new Research(PaleMultiplyReconstruction)),()->{
                         node(saint,Seq.with(new Research(PaleExponentReconstruction)),()->{
                            node(bloodLotus,Seq.with(new Research(PaleImmeasurableReconstruction)),()->{

                            });
                         });
                      });
                   });
               });
               node(PaleUnitFactory,()->{
                   node(PaleNumberReconstruction,()->{
                      node(PaleMultiplyReconstruction,()->{
                          node(PaleExponentReconstruction,()->{
                             node(PaleImmeasurableReconstruction,()->{
                                node(GodForsakenParts,()->{

                                });
                                node(FatedParts,()->{

                                });
                                node(StandaloneParts,()->{

                                });
                                node(CalamityParts,()->{

                                });
                             });
                          });
                      });
                   });
               });
            });
            //power
            node(OriginalElectronics,()->{
               node(SteamElectronics,()->{
                  node(RestoreMotor,()->{

                  });
                  node(RadioisotopeGenerator,()->{
                     node(DepletedUraniumPower,()->{
                         node(UraniumPowerAppliance,()->{

                         });
                     });
                  });
               });
               node(PowerCapacitor,()->{
                  node(FloatingCapacitor,()->{
                     node(DepletedUraniumCapacitor,()->{

                     });
                  });
               });
               node(ElectricalNode,()->{

               });
            });
            //wall
            node(bigIronWall,()->{
               node(bigIronLargeWall,()->{

               });
               node(energyStorageWall,()->{
                  node(energyStorageLargeWall,()->{

                  });
                  node(IllusionGate,()->{
                     node(IllusionLargeGate,()->{

                     });
                  });
                  node(magneticPullWall,()->{
                     node(magneticPullLargeWall,()->{

                     });
                  });
               });
               node(frailPolyesterWall,()->{
                  node(frailPolyesterLargeWall,()->{

                  });
               });
               node(floatWall,()->{
                  node(floatLargeWall,()->{

                  });
                  node(rubberWall,()->{
                      node(rubberLargeWall,()->{

                      });
                  });
               });
               node(alkSliverWall,()->{
                  node(alkSliverLargeWall,()->{

                  });
               });
               node(thallideWall,()->{
                  node(thallideLargeWall,()->{

                  });
               });
               node(uraniumWall,()->{
                  node(uraniumLargeWall,()->{

                  });
                  node(energyShield,()->{

                  });
               });
            });
            //effect
            node(FederalSubCore);
            node(JuniorMender,()->{
               node(MenderProjector,()->{
                   node(SeniorMender,()->{

                   });
                   node(DefenceShieldProjector,()->{
                   });
                   node(OverloadDefenceTower,()->{
                   });
                   node(ConstructionField,()->{
                   });
               });
               node(FederalContainer,()->{
                  node(FederalWarehouse,()->{

                  });
               });
               node(antiStealthRadar,()->{
                  node(BulletAccelerator,()->{

                  });
               });
               node(OverloadedThrowor,()->{
                  node(SeniorOverloaded,()->{
                  });
               });
            });
            //transport
            node(bigIronDuct,()->{
                node(bigIronRouter,()->{
                    node(basicUnloader,()->{
                    });
                });
                node(floatDuct,()->{
                    node(floatRouter,()->{

                    });
                    node(floatOverFlow,()->{

                    });
                    node(floatUnderFlow,()->{

                    });
                    node(floatJunction,()->{

                    });
                    node(floatBridge,()->{

                    });
                    node(SwiftConveyor,()->{
                       node(UnifiedDrive,()->{

                       });
                    });
                });
                node(bigIronOverFlow,()->{

                });
                node(bigIronUnderFlow,()->{

                });
                node(bigIronJunction,()->{

                });
                node(bigIronBridge,()->{

                });
                node(ThermalConductor,()->{
                   node(GaintThermalConductor,()->{
                      node(UnitCarryingPoint,()->{
                          node(UnitUnloadingContainer,()->{

                          });
                      });
                   });
                });
                node(OrganicConduit,()->{
                   node(OrganicJunction,()->{
                       node(OrganicRouter,()->{

                       });
                       node(OrganicBridge,()->{

                       });
                   });
                   node(FloatConduic,()->{
                       node(FloatJunction,()->{

                       });
                       node(FloatBridge,()->{

                       });
                       node(FloatRouter,()->{
                           node(FloatTank,()->{

                           });
                       });
                   });
                });
            });
            //turret
            node(DefeatGod,()->{
               node(StandingGround,()->{
                   node(Joy,()->{
                       node(Traction,()->{

                       });
                   });
               });
                node(TraceSource,()->{
                    node(CrossTractor,()->{

                    });
                });
                node(Wanuo,()->{
                   node(Kurao,()->{
                       node(CircuitBreak,()->{
                           node(IntermittentPressure,()->{

                           });
                       });
                   });
                   node(MPI,()->{
                       node(BreathSoil,()->{
                           node(TurbidAir,()->{

                           });
                       });
                   });
                   node(NonSeen,()->{
                       node(Mountains,()->{
                           node(EverVictorious,()->{

                           });
                       });
                       node(MistRosy,()->{
                           node(DivineCreation,()->{

                           });
                           node(FilthySin,()->{

                           });
                       });
                   });
               });
               node(Incinerate,()->{
                   node(BurnInjured,()->{

                   });
               });
            });
            //drills
            node(bigIronDrill,()->{
                node(floatDrill,()->{
                    node(nuclearDrill,()->{

                    });
                });
                node(hotMeltDrill,()->{
                    node(wallCrusher,()->{
                        node(rubberCrusher,()->{
                           node(uranCrystalCrusher,()->{

                           });
                        });
                    });
                });
                node(waterSamplingDevice,()->{
                    node(fifthCoagulator,()->{

                    });
                });
                node(bigIronPump,()->{
                   node(floatPump,()->{
                      node(nuclearPowerPump,()->{

                      });
                   });
                });
            });
            //crafting
            node(CompressionChamber,()->{
                node(distillationRoom,()->{

                });
               node(monoSiliCrystalFactory,()->{
                   node(contaminationRoom,()->{
                       node(dirtyDecompositionRoom,()->{

                       });
                   });
                   node(nuclearFluidCollector,()->{
                       node(uraniumPurificationRoom,()->{
                           node(UraniumPrecipitationRoom,()->{
                           });
                       });
                   });
                   node(sacredIronEngravingRoom,()->{
                       node(prismSmasher,()->{
                           node(backflowReversalRoom,()->{

                           });
                           node(divineTearsEnrichRoom,()->{

                           });
                       });
                   });
               });
               node(MagentPurifier,()->{
                   node(MagentEnergyStation,()->{});
                   node(silverPlatingRoom,()->{
                      node(Grinder,()->{
                          node(MagenticStormStabiliser,()->{

                          });
                      });
                   });
               });
               node(coinProducer,()->{
                  node(exchange,()->{

                  });
               });
               node(HeatMaker,()->{
                   node(strangeLiquidExtractionRoom,()->{
                       node(thalliumCompoundCrucible,()->{

                       });
                       node(OxygenLiquefactionRoom,()->{

                       });
                   });
                   node(seedCollector,()->{
                       node(rubberGrower,()->{

                       });
                   });
                   node(heatRelaxation,()->{

                   });
               });
            });
            //items,liquids
            nodeProduce(bigIron,()->{
                nodeProduce(Liquids.water,()->{
                    nodeProduce(dirtySolution,()->{
                        nodeProduce(nuclearFluid,()->{

                        });
                    });
                    nodeProduce(strangeLiquid,()->{
                       nodeProduce(liquidOxygen,()->{

                       });
                    });
                    nodeProduce(prismLiquid,()->{
                       nodeProduce(divineTears,()->{

                       });
                    });
                });
                nodeProduce(sand,()->{
                    nodeProduce(monoSiliCrystal,()->{

                    });
                });
                nodeProduce(Tcoal,()->{
                   nodeProduce(graphite,()->{

                   });
                   nodeProduce(dirtyCoagulum,()->{

                   });
                   nodeProduce(frailPolyester,()->{
                       nodeProduce(sulFurFrag,()->{
                          nodeProduce(pyratite,()->{

                          });
                       });
                       nodeProduce(oriRubber,()->{
                          nodeProduce(rubberFrag,()->{
                              nodeProduce(rubber,()->{

                              });
                          });
                       });
                   });
                   nodeProduce(pumice,()->{
                      nodeProduce(alkSliver,()->{

                      });
                   });
                   nodeProduce(thallium,()->{
                       nodeProduce(thallide,()->{

                       });
                   });
                   nodeProduce(uranCrystal,()->{
                      nodeProduce(oriUranium,()->{
                         nodeProduce(uranium,()->{
                         });
                      });
                   });
                   nodeProduce(prismCrystal,()->{
                      nodeProduce(sacredIron,()->{
                         nodeProduce(remakeSource,()->{

                         });
                      });
                   });
                });
                nodeProduce(magent,()->{
                   nodeProduce(bottledMagenticStorm,()->{

                   });
                });
            });
        });
    }
}
