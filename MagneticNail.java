package cn.com.nbport.zgt.demo.simulation.map;
//磁钉
public class MagneticNail {
    // 磁钉唯一标识
    private String id;
    // x轴坐标
    private int x;
    // y轴坐标
    private int y;

    // 无参构造
    public MagneticNail() {}
    public MagneticNail(String id, int x, int y) {
        this.id = id;
        this.x = x;
        this.y = y;
    }
    // 计算当前磁钉到目标的距离
    public double distanceTo(MagneticNail target) {
        int dx = this.x - target.x;
        int dy = this.y - target.y;
        return Math.sqrt(dx * dx + dy * dy);
    }
    // 获取磁钉ID
    public String getId() { return id; }
    // 设置磁钉ID
    public void setId(String id) { this.id = id; }
    // 获取x轴坐标
    public int getX() { return x; }
    // 设置x轴坐标
    public void setX(int x) { this.x = x; }
    // 获取y轴坐标
    public int getY() { return y; }
    // 设置y轴坐标
    public void setY(int y) { this.y = y; }
}
