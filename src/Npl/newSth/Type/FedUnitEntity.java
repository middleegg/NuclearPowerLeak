package Npl.newSth.Type;

import arc.func.Prov;
import arc.struct.ObjectIntMap;
import arc.util.Strings;
import mindustry.gen.EntityMapping;
import mindustry.gen.Entityc;

/**
 * 自定义 Entity 注册工具（模仿 PU132 的 UnityEntityMapping）。
 *
 * <h3>为什么需要这个类？</h3>
 * <p>Mindustry v154.3 起要求每个自定义 Entity 子类都拥有唯一的 classId，
 * 否则在 {@code UnitType.init()} 阶段会抛 {@link ClassCastException}，
 * 表现为「单位一召唤游戏就崩」或「mod 加载时报错」。
 *
 * <p>但 arc 框架的 {@link EntityMapping} 并没有暴露「注册并返回 id」的 API，
 * 只有一个 {@code idMap} 数组和一个 {@code nameMap}。所以我们自己实现一个
 * 小工具：扫描 {@code idMap} 找一个空 slot，把构造器塞进去，再用一张
 * {@code Class → int} 的映射记录 classId。
 *
 * <h3>使用流程（在 mod 的 loadContent 阶段）</h3>
 * <pre>{@code
 * // 1. 注册：把自定义 Entity 类和它的构造器 Prov 放进 idMap
 * ZEntityRegister.register(SegmentWormEntity.class, SegmentWormEntity::new);
 * ZEntityRegister.register(SegmentUnitEntity.class, SegmentUnitEntity::new);
 *
 * // 2. 在 Entity 子类内重写 classId()，返回注册时拿到的 id
 * @Override public int classId() {
 *     return ZEntityRegister.classId(SegmentWormEntity.class);
 * }
 * }</pre>
 *
 * <h3>本精简版说明</h3>
 * <p>原项目注册了 SegmentWormEntity、SegmentUnitEntity、EndGroundUnit、CopterUnitEntity 等
 * 多种 Entity。本精简版只保留 SegmentWormEntity（虫子头部）和 SegmentUnitEntity（虫子段身），
 * 其他与多节单位无关的 Entity 已移除。
 */
public class FedUnitEntity {
    /** {@code type → classId} 映射，记录每个已注册类的 id，供 {@link #classId(Class)} 反查 */
    private static final ObjectIntMap<Class<? extends Entityc>> ids = new ObjectIntMap<>();
    /**
     * 下一个待扫描的 slot 游标。
     * <p>注册时只往后扫，不从头扫，避免每次都重复检查前面已经占用的 slot。
     * arc 默认 idMap 留有很多空位，cursor 单调递增不会回退。
     */
    private static int cursor = 0;

    /**
     * 注册一个 Entity 类，返回它分配到的 classId。
     *
     * <p>同一个类重复注册不会重新分配 id，直接返回已分配的值（幂等），
     * 这在 mod 重载场景下很重要。
     *
     * @param type Entity 子类的 Class 对象
     * @param prov Entity 的构造器（通常 {@code SomeEntity::new}）
     * @param <T>  Entity 类型
     * @return 分配到的 classId（即 idMap 中的 slot 索引）
     * @throws RuntimeException 如果 idMap 已满（极少见，arc 默认留很多空位）
     */
    public static synchronized <T extends Entityc> int register(Class<T> type, Prov<T> prov) {
        if (ids.containsKey(type)) return ids.get(type, -1);

        // 从 cursor 开始往后找一个空 slot
        for (; cursor < EntityMapping.idMap.length; cursor++) {
            if (EntityMapping.idMap[cursor] == null) {
                EntityMapping.idMap[cursor] = prov;
                ids.put(type, cursor);

                // 同时注册到 nameMap：UnitType 构造时会用 EntityMapping.map(name) 查 prov，
                // 这里把「类名」和「kebab-case 名」都注册一遍，保证两种命名都能查到
                EntityMapping.nameMap.put(type.getSimpleName(), prov);
                EntityMapping.nameMap.put(Strings.camelToKebab(type.getSimpleName()), prov);

                return cursor;
            }
        }

        // 没有空 slot 了（一般不会发生）
        throw new RuntimeException("No free entity id slot for " + type.getSimpleName());
    }

    /**
     * 获取已注册的 classId。
     *
     * @param type 已通过 {@link #register} 注册过的 Entity 类
     * @return classId；未注册过返回 -1
     */
    public static int classId(Class<? extends Entityc> type) {
        return ids.get(type, -1);
    }
}
