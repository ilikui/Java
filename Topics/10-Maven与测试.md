# Maven、JUnit 与工程化

## Maven 的职责

Maven 使用 POM 描述项目、依赖和构建配置。常用生命周期阶段包括 validate、compile、test、package、verify、install、deploy；执行某个阶段会先执行该生命周期中此前的阶段。

常见命令：

```bash
mvn clean
mvn test
mvn package
mvn verify
```

具体命令是否成功取决于项目配置、JDK 版本、依赖仓库和测试环境。

## 项目结构约定

```text
src/
  main/
    java/
    resources/
  test/
    java/
    resources/
pom.xml
README.md
```

将业务代码与测试代码分开，避免把本地凭据提交到仓库。依赖版本应显式管理并定期检查安全更新。

## 单元测试的结构

一个清晰测试通常包含准备（Arrange）、执行（Act）、断言（Assert）。测试名称应说明场景和预期结果，而不是只写 test1。

```java
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class CalculatorTest {
    @Test
    void addsTwoPositiveNumbers() {
        assertEquals(5, 2 + 3);
    }
}
```

该例演示 JUnit 5 的基本形式。实际项目应测试自己的业务方法，而不是只测试语言运算符。

## 测试层次

- 单元测试：隔离验证小范围逻辑。
- 集成测试：验证模块、数据库或外部组件之间的协作。
- 端到端测试：从外部入口验证完整业务流程。

测试应覆盖正常路径、边界值、无效输入和异常路径；不要让测试依赖不可控的网络或真实生产数据。

## 常见故障排查

1. 确认 `java -version` 与 `mvn -version` 指向预期 JDK。
2. 查看最早出现的编译或测试错误，不只看最后一行失败摘要。
3. 检查依赖坐标、仓库配置和网络代理。
4. 检查测试是否依赖时区、系统时间、随机数或执行顺序。
5. 让构建可在干净环境中复现。

## 面试自测

- Maven 的生命周期与插件有什么关系？
- test、package、verify 有何区别？
- 单元测试与集成测试如何划分？
- 什么是依赖传递与依赖冲突？
