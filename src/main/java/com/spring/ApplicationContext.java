package com.spring;

import com.caoliang.AppConfig;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;

public class ApplicationContext {

    private Class configClass;

    private ConcurrentHashMap<String,Object> singletonObjects = new ConcurrentHashMap<>(); //单例池

    private ConcurrentHashMap<String,BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>();



    public ApplicationContext(Class configClass) {

        this.configClass = configClass;

        //解析配置类
        //ComponentScan - 扫描路径 - 扫描 -- BeanDefinition -- beanDefinitionMap
        scan(configClass);

        for (String beanName : beanDefinitionMap.keySet()) {
            BeanDefinition beanDefinition = beanDefinitionMap.get(beanName);
            if(beanDefinition.getScope().equals("singleton")){
                Object bean = createBean(beanDefinition);                       //单例bean
                singletonObjects.put(beanName,bean);
            }
        }

    }

    public Object createBean(BeanDefinition beanDefinition){

        Class clazz = beanDefinition.getClazz();
        try {
            Object instance = clazz.getDeclaredConstructor().newInstance();

            //依赖注入
            for (Field field : clazz.getDeclaredFields()) {
                if(field.isAnnotationPresent(Autowired.class)){
                    Object bean = getBean(field.getName());
                    field.setAccessible(true);
                    field.set(instance,bean);
                }
            }

            return instance;
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }

        return null;

    }

    private void scan(Class configClass) {
        //ComponentScan - 扫描路径 - 扫描
        ComponentScan componentScanAnnotation = (ComponentScan) configClass.getDeclaredAnnotation(ComponentScan.class);
        String path = componentScanAnnotation.value();

        path = path.replace(".","/");

        ClassLoader classLoader = ApplicationContext.class.getClassLoader();

        URL resource = classLoader.getResource(path);
        File file = new File(resource.getFile());

        if(file.isDirectory()){
            File[] files = file.listFiles();

            for (File f : files) {
                String absolutePath = f.getAbsolutePath();
                if(absolutePath.endsWith(".class")){
                    String className = absolutePath.substring(absolutePath.indexOf("com"), absolutePath.indexOf(".class"));
                    className = className.replace("/",".");

                    System.out.println(className);

                    try {
                        Class<?> clazz =  classLoader.loadClass(className);
                        //表示是一个bean
                        if(clazz.isAnnotationPresent(Component.class)){
                            Component componentAnno = clazz.getDeclaredAnnotation(Component.class);
                            String beanName = componentAnno.value();

                            //beanDefinition
                            BeanDefinition beanDefinition = new BeanDefinition();

                            beanDefinition.setClazz(clazz);

                            //判断是单例bean 还是 多例bean
                            if(clazz.isAnnotationPresent(Scope.class)){
                                Scope scopeAnno = clazz.getDeclaredAnnotation(Scope.class);
                                beanDefinition.setScope(scopeAnno.value());
                            }else{
                                beanDefinition.setScope("singleton");
                            }

                            beanDefinitionMap.put(beanName,beanDefinition);

                        }
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }

                }
            }
        }
    }

    public Object getBean(String beanName){
        if(beanDefinitionMap.containsKey(beanName)){
            BeanDefinition beanDefinition = beanDefinitionMap.get(beanName);
            if(beanDefinition.getScope().equals("singleton")){
                Object o = singletonObjects.get(beanName);
                return o;
            }else{
                //创建bean对象
                Object bean = createBean(beanDefinition);
                return bean;
            }
        }else {
            throw new NullPointerException("不存在对应的Bean");
        }
    }


}
