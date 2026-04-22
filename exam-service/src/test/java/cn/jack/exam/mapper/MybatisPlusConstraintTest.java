package cn.jack.exam.mapper;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MybatisPlusConstraintTest {

    @Test
    void adminMappersShouldUseMybatisPlusBaseMapper() throws Exception {
        Class<?> baseMapperClass = Class.forName("com.baomidou.mybatisplus.core.mapper.BaseMapper");

        assertTrue(baseMapperClass.isAssignableFrom(AdminUserMapper.class));
        assertTrue(baseMapperClass.isAssignableFrom(AdminSessionMapper.class));
        assertTrue(baseMapperClass.isAssignableFrom(AdminRoleMapper.class));
        assertTrue(baseMapperClass.isAssignableFrom(AdminPermissionMapper.class));
        assertTrue(baseMapperClass.isAssignableFrom(PaperMapper.class));
        assertTrue(baseMapperClass.isAssignableFrom(PaperQuestionMapper.class));
    }
}
