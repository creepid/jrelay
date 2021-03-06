package com.github.jrelay;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * Created by m.rusakovich on 26.04.2016.
 */
public class RelayDriverUtils{

        private static final Logger LOG = LoggerFactory.getLogger(RelayDriverUtils.class);

        private RelayDriverUtils() {
        }

        /**
         * Find relay driver. Scan packages to search drivers specified in the
         * argument.
         *
         * @param names array of driver names to search for
         * @return Driver if found or throw exception
         * @throw RelayException
         */
        protected static RelayDriver findDriver(List<String> names, List<Class<?>> classes) {

                for (String name : names) {

                        LOG.info("Searching driver {}", name);

                        Class<?> clazz = null;

                        for (Class<?> c : classes) {
                                if (c.getCanonicalName().equals(name)) {
                                        clazz = c;
                                        break;
                                }
                        }

                        if (clazz == null) {
                                try {
                                        clazz = Class.forName(name);
                                } catch (ClassNotFoundException e) {
                                        LOG.trace("Class not found {}, fall thru", name);
                                }
                        }

                        if (clazz == null) {
                                LOG.debug("Driver {} not found", name);
                                continue;
                        }

                        LOG.info("Relay driver {} has been found", name);

                        try {
                                return (RelayDriver) clazz.newInstance();
                        } catch (InstantiationException e) {
                                throw new RuntimeException(e);
                        } catch (IllegalAccessException e) {
                                throw new RuntimeException(e);
                        }
                }

                return null;
        }

        /**
         * Scans all classes accessible from the context class loader which belong
         * to the given package and subpackages.
         *
         * @param pkgname The base package
         * @param flat scan only one package level, do not dive into subdirectories
         * @return The classes
         * @throws ClassNotFoundException
         * @throws IOException
         */
        protected static Class<?>[] getClasses(String pkgname, boolean flat) {

                List<File> dirs = new ArrayList<File>();
                List<Class<?>> classes = new ArrayList<Class<?>>();

                ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
                String path = pkgname.replace('.', '/');

                Enumeration<URL> resources = null;
                try {
                        resources = classLoader.getResources(path);
                } catch (IOException e) {
                        throw new RuntimeException("Cannot read path " + path, e);
                }

                while (resources.hasMoreElements()) {
                        URL resource = resources.nextElement();
                        dirs.add(new File(resource.getFile()));
                }

                for (File directory : dirs) {
                        try {
                                classes.addAll(findClasses(directory, pkgname, flat));
                        } catch (ClassNotFoundException e) {
                                throw new RuntimeException("Class not found", e);
                        }
                }

                return classes.toArray(new Class<?>[classes.size()]);
        }

        /**
         * Recursive method used to find all classes in a given directory and
         * subdirectories.
         *
         * @param dir base directory
         * @param pkgname package name for classes found inside the base directory
         * @param flat scan only one package level, do not dive into subdirectories
         * @return Classes list
         * @throws ClassNotFoundException
         */
        private static List<Class<?>> findClasses(File dir, String pkgname, boolean flat) throws ClassNotFoundException {
                List<Class<?>> classes = new ArrayList<Class<?>>();
                if (!dir.exists()) {
                        return classes;
                }

                File[] files = dir.listFiles();
                for (File file : files) {
                        if (file.isDirectory() && !flat) {
                                classes.addAll(findClasses(file, pkgname + "." + file.getName(), flat));
                        } else if (file.getName().endsWith(".class")) {
                                classes.add(Class.forName(pkgname + '.' + file.getName().substring(0, file.getName().length() - 6)));
                        }
                }

                return classes;
        }

}
