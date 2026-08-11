package Npl.newSth.Type;

import mindustry.gen.UnitEntity;

/**
 * 多节段虫子单位的「头部」实体（骨架版）。
 *
 * <p>当前只做最小实现：让 {@link FedUnitEntity#register} 能拿到 classId、
 * 让 {@code SegmentWormEntity::create} 能作为 UnitType 的 constructor 使用。
 * 后续要补的虫头逻辑（领导段身、记仇回传、 WormAI 接线等）先留空。
 */
public class SegmentWormEntity extends UnitEntity {

    /** UnitType.constructor 用：返回一个新实例。 */
    public static SegmentWormEntity create() {
        return new SegmentWormEntity();
    }

    @Override
    public int classId() {
        return FedUnitEntity.classId(SegmentWormEntity.class);
    }
}
