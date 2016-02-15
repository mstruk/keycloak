/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.testsuite.util;

import org.junit.Ignore;
import org.junit.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.services.DefaultKeycloakSessionFactory;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class ClientServerRestDiffTest {

    static HashSet<Class> endpointVerbs = new HashSet<>();

    static {
        endpointVerbs.add(GET.class);
        endpointVerbs.add(POST.class);
        endpointVerbs.add(PUT.class);
        endpointVerbs.add(DELETE.class);
        endpointVerbs.add(HEAD.class);
        endpointVerbs.add(OPTIONS.class);
    }


    @Ignore
    @Test
    public void findMissingEndpoints() throws Exception {

        EndpointSurveyor clientSurveyor = new EndpointSurveyor();
        clientSurveyor.performSurvey(Keycloak.class);

        EndpointSurveyor serverSurveyor = new EndpointSurveyor();
        serverSurveyor.performSurvey(DefaultKeycloakSessionFactory.class);

        // we can now perform some analysis
        // first filter all endpoints to those starting with /admin
        // and sort them
        Map<String, List<EndpointInfo>> cliFiltered = clientSurveyor.getEndpoints().entrySet().stream()
                .filter(e -> e.getKey().startsWith("/admin/"))
                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
        cliFiltered = new TreeMap<>(cliFiltered);

        Map<String, List<EndpointInfo>> srvFiltered = serverSurveyor.getEndpoints().entrySet().stream()
                .filter(e -> e.getKey().startsWith("/admin/"))
                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
        srvFiltered = new TreeMap<>(srvFiltered);

        // now we need 2 lists
        LinkedHashMap<String, List<EndpointInfo>> removed = new LinkedHashMap<>();
        LinkedHashMap<String, List<EndpointInfo>> added = new LinkedHashMap<>();

        // one called removed with endpoints that are in cliFiltered but not srvFiltered
        // one called added with endpoints that are in srvFiltered but not cliFiltered
        Iterator<String> it = cliFiltered.keySet().iterator();
        Iterator<String> it2 = srvFiltered.keySet().iterator();

        int next = 0;
        String c = null;
        String s = null;
        while (it.hasNext() || it2.hasNext()) {
            if (next <= 0) {
                if (!it.hasNext())
                    break;
                c = it.next();
            }
            if (next >= 0) {
                if (!it2.hasNext())
                    break;
                s = it2.next();
            }
            int cmp = c.compareTo(s);
            if (cmp < 0) {
                removed.put(c, cliFiltered.get(c));
                next = -1;
            } else if (cmp > 0) {
                added.put(s, srvFiltered.get(s));
                next = 1;
            } else {
                compareMethods(c, cliFiltered, s, srvFiltered, removed, added);
                next = 0;
            }
        }

        // one has run out so all other items go into the same list
        while(it.hasNext()) {
            c = it.next();
            removed.put(c, cliFiltered.get(c));
        }
        while(it2.hasNext()) {
            s = it2.next();
            added.put(s, srvFiltered.get(s));
        }

        // let's do another analysis
        // we are interested in endpoint impls that take or return types that end by Model
        Map<String, List<EndpointInfo>> all = serverSurveyor.getEndpoints();
        for (Map.Entry<String, List<EndpointInfo>> entry: all.entrySet()) {
            for (EndpointInfo ei: entry.getValue()) {
                String type = ei.getReturnType().getName();
                if (type.endsWith("Model")) {
                    System.out.println(ei);
                    continue;
                }
                Method m = ei.getMethod();
                Class<?>[] types = m.getParameterTypes();
                for (Class cl: types) {
                    if (cl.getName().endsWith("Model")) {
                        System.out.println(ei);
                        break;
                    }
                }
                //System.out.println("- " + m);
            }
        }

        printReport(added, removed);

        System.out.println("Done.");
    }

    private void printReport(LinkedHashMap<String, List<EndpointInfo>> added, LinkedHashMap<String, List<EndpointInfo>> removed) {

        System.out.println("Endpoints declared in admin-client not matched on the services REST API:");
        printPartialReport(removed);

        System.out.println();
        System.out.println();
        System.out.println("Endpoints declared in services REST API not matched in admin-client:");
        printPartialReport(added);
    }

    private void printPartialReport(LinkedHashMap<String, List<EndpointInfo>> calls) {
        int count = 1;
        for (Map.Entry<String, List<EndpointInfo>> ent: calls.entrySet()) {
            for (EndpointInfo ei: ent.getValue()) {
                System.out.println();
                System.out.printf("%d.  %7s %s\n", count++, ei.getVerb(), ei.getFullPath());
                System.out.println("            " + formatType(ei.getMethod().getDeclaringClass()) + " :: " + formatType(ei.getMethod().getReturnType()) + " " + ei.getMethod().getName() + " (" + formatListOfTypes(Arrays.asList(ei.getMethod().getParameterTypes())) + ")");
                System.out.println("               query params: " + ei.getQueryParams());
                System.out.println("                body params: " + formatListOfTypes(ei.getBodyParams()));
            }
        }
    }

    private String formatListOfTypes(List<Class> types) {
        StringBuilder sb = new StringBuilder();
        for (Class<?> type: types) {
            if (sb.length() > 0)
                sb.append(", ");
            sb.append(formatType(type));
        }
        return sb.toString();
    }

    private String formatType(Class<?> type) {
        return type.getName();
    }

    private void compareMethods(String c, Map<String, List<EndpointInfo>> cliFiltered, String s, Map<String, List<EndpointInfo>> srvFiltered,
                                Map<String, List<EndpointInfo>> removedPaths, Map<String, List<EndpointInfo>> addedPaths) {

        MethodComparator criteria = new MethodComparator();
        List<EndpointInfo> ceps = sort(cliFiltered.get(c), criteria);
        List<EndpointInfo> seps = sort(srvFiltered.get(s), criteria);

        List<EndpointInfo> removed = new LinkedList<>();
        List<EndpointInfo> added = new LinkedList<>();

        int next = 0;
        EndpointInfo a = null;
        EndpointInfo b = null;

        Iterator<EndpointInfo> it = ceps.iterator();
        Iterator<EndpointInfo> it2 = seps.iterator();
        while (it.hasNext() || it2.hasNext()) {
            if (next <= 0) {
                if (!it.hasNext())
                    break;
                a = it.next();
            }
            if (next >= 0) {
                if (!it2.hasNext())
                    break;
                b = it2.next();
            }
            int cmp = criteria.compare(a, b);
            if (cmp < 0) {
                removed.add(a);
                next = -1;
            } else if (cmp > 0) {
                added.add(b);
                next = 1;
            } else {
                next = 0;
            }
        }
        // one has run out so all other items go into the same list
        while(it.hasNext()) {
            removed.add(it.next());
        }
        while(it2.hasNext()) {
            added.add(it2.next());
        }

        if (removed.size() > 0) {
            removedPaths.put(c, removed);
        }
        if (added.size() > 0) {
            addedPaths.put(s, removed);
        }
    }

    private <T> List<T> sort(List<T> list, MethodComparator cmp) {
        ArrayList<T> tmp = new ArrayList<>(list);
        Collections.sort(tmp, cmp);
        return tmp;
    }

    static class MethodComparator<T> implements Comparator<EndpointInfo> {

        @Override
        public int compare(EndpointInfo o1, EndpointInfo o2) {
            if (o1 == null) return -1;
            if (o2 == null) return 1;

            Class rtype1 = o1.getReturnType();
            Class rtype2 = o2.getReturnType();
            rtype1 = rtype1 == Response.class ? void.class : rtype1;
            rtype2 = rtype2 == Response.class ? void.class : rtype2;
            if (rtype1 != rtype2) {
                return rtype1 == null ? -1 : rtype2 == null ? 1 : rtype1.getName().compareTo(rtype2.getName());
            }
            int cmp = o1.getVerb().compareTo(o2.getVerb());
            if (cmp != 0) {
                return cmp;
            }

            Set<String> c1 = o1.getConsumes();
            Set<String> c2 = o2.getConsumes();
            if (c1.size() != 0) return compareList(new ArrayList(new TreeSet(c1)), new ArrayList(new TreeSet(c2)));
            if (c2.size() != 0) return -1;

            Set<String> p1 = o1.getProduces();
            Set<String> p2 = o2.getProduces();
            if (p1.size() != 0) return compareList(new ArrayList(new TreeSet(p1)), new ArrayList(new TreeSet(p2)));
            if (p2.size() != 0) return -1;

            // they are already ordered
            List<? extends Comparable> qparams1 = o1.getQueryParams();
            List<? extends Comparable> qparams2 = o2.getQueryParams();

            cmp = compareList(qparams1, qparams2);
            if (cmp != 0) return cmp;

            // they are already ordered
            List<? extends Object> bparams1 = o1.getBodyParams();
            List<? extends Object> bparams2 = o2.getBodyParams();

            cmp = compareListByToString(bparams1, bparams2);
            if (cmp != 0) return cmp;

            return o1.getMethod().toString().compareTo(o2.getMethod().toString());
        }
    }

    static int compareListByToString(List<? extends Object> l1, List<? extends Object> l2) {
        if (l1 != null) {
            if (l2 != null) {
                if (l1.size() != l2.size()) {
                    return l1.size() < l2.size() ? -1 : 1;
                } else {
                    for (int i=0; i < l1.size(); i++) {
                        int cmp = l1.get(i).toString().compareTo(l2.get(i).toString());
                        if (cmp != 0) {
                            return cmp;
                        }
                    }
                }
            } else {
                return 1;
            }
        } else if (l2 != null) {
            return -1;
        }
        return 0;
    }

    static int compareList(List<? extends Comparable> l1, List<? extends Comparable> l2) {
        if (l1 != null) {
            if (l2 != null) {
                if (l1.size() != l2.size()) {
                    return l1.size() < l2.size() ? -1 : 1;
                } else {
                    for (int i=0; i < l1.size(); i++) {
                        int cmp = l1.get(i).compareTo(l2.get(i));
                        if (cmp != 0) {
                            return cmp;
                        }
                    }
                }
            } else {
                return 1;
            }
        } else if (l2 != null) {
            return -1;
        }
        return 0;
    }

    static class EndpointSurveyor {

        Map<Class, ConfigInfo> register = new HashMap<>();
        Map<String, List<EndpointInfo>> endpoints = new HashMap<>();

        public Map<Class, ConfigInfo> getRegister() {
            return register;
        }

        public Map<String, List<EndpointInfo>> getEndpoints() {
            return endpoints;
        }

        void performSurvey(Class clazz) throws Exception {
            ClassLoader cl = clazz.getClassLoader();
            List<String> classList = null;

            String pathToClass = classToPath(clazz);
            URL classFile = cl.getResource(pathToClass);

            if ("file".equals(classFile.getProtocol())) {
                Path root = Paths.get(classFile.toURI());
                int pkgDepth = Paths.get(pathToClass).getNameCount();
                for (int i = 0; i < pkgDepth; i++) {
                    root = root.getParent();
                }
                classList = getClassList(root.toString());
            }

            //System.out.println("List of classes for " + clazz + ": " + classList);
            for (String name: classList) {

                Class claz = cl.loadClass(name);
                ConfigInfo config = register.get(claz);
                if (config == null) {
                    config = new ConfigInfo(claz);
                    register.put(claz, config);
                }

                String path = asPath(claz.getAnnotation(javax.ws.rs.Path.class));
                if (path != null) {
                    //System.out.println("Found @Path: " + name);
                    config.setPath(path);
                }

                String [] consumes = asConsumes(claz.getAnnotation(javax.ws.rs.Consumes.class));
                if (consumes != null) {
                    //System.out.println("Found @Consumes: " + Arrays.asList(consumes));
                    config.setConsumes(consumes);
                }

                String [] produces = asProduces(claz.getAnnotation(javax.ws.rs.Produces.class));
                if (produces != null) {
                    //System.out.println("Found @Produces: " + Arrays.asList(produces));
                    config.setProduces(produces);
                }

                // now iterate over methods
                Method[] methods = claz.getMethods();
                for (Method m: methods) {
                    List<String> verbs = new LinkedList<>();
                    String mpath = null;
                    String [] csumes = null;
                    String [] pduces = null;

                    Annotation[] anns = m.getAnnotations();
                    for (Annotation an: anns) {
                        String val = asHttpVerb(an);
                        if (val != null) {
                            verbs.add(val);
                            continue;
                        }
                        val = asPath(an);
                        if (val != null) {
                            mpath = val;
                            continue;
                        }
                        String [] vals = asConsumes(an);
                        if (vals != null) {
                            csumes = vals;
                            continue;
                        }
                        vals = asProduces(an);
                        if (vals != null) {
                            pduces = vals;
                            continue;
                        }
                    }

                    List<String> namedParams = new LinkedList<>();
                    List<Class> bodyParams = new LinkedList<>();

                    Class [] ptypes = m.getParameterTypes();
                    Annotation[][] panns = m.getParameterAnnotations();
                    for (int i = 0; i < ptypes.length; i++) {
                        Class c = ptypes[i];
                        String named = null;
                        boolean skip = false;
                        if (panns[i].length > 0) {
                            for (Annotation a: panns[i]) {
                                if (a.annotationType().equals(PathParam.class)) {
                                    named = ((PathParam) a).value();
                                    break;
                                } else if (a.annotationType().equals(Context.class)) {
                                    skip = true;
                                }
                            }
                        }
                        if (named != null) {
                            namedParams.add(named);
                        } else if(!skip) {
                            bodyParams.add(c);
                        }
                    }
                    //System.out.println("Huh? " + panns);

                    if (verbs.size() > 0) {
                        // it is an endpoint
                        for (String verb: verbs) {
                            EndpointInfo endpoint = new EndpointInfo(config, m, verb, mpath, csumes, pduces, namedParams, bodyParams);
                            config.getEndpoints().add(endpoint);
                        }
                    } else if (mpath != null) {
                        // @Path without verb means the return value is a resource
                        Class rtype = m.getReturnType();
                        ConfigInfo res = register.get(rtype);
                        if (res == null) {
                            res = new ConfigInfo(rtype);
                            register.put(rtype, res);
                        }
                        res.setPath(mpath);
                        res.setParent(config);
                    }
                }
            }

            // now register endpoints - cannot calculate full paths until all ConfigInfo are processed
            for (ConfigInfo conf: register.values()) {
                for (EndpointInfo ei: conf.getEndpoints()) {
                    List<EndpointInfo> endpts = endpointsForPath(ei.getFullPath());
                    endpts.add(ei);
                }
            }
        }

        static String classToPath(Class clazz) {
            return clazz.getName().replace('.', '/') + ".class";
        }

        private List<String> getClassList(String root) throws IOException {
            List<String> classList = new LinkedList<>();
            addClasses(Paths.get(root), Paths.get(root), classList);
            return classList;
        }

        private void addClasses(Path root, Path path, List<String> classList) throws IOException {
            for (Path child: Files.list(path).collect(Collectors.<Path>toList())) {
                if (Files.isDirectory(child)) {
                    addClasses(root, child, classList);
                } else if (child.toString().endsWith(".class")) {
                    Path lastSegment = child.getFileName();
                    if (lastSegment.toString().contains("$")) {
                        continue;
                    } else {
                        String className = root.relativize(child).toString();
                        className = className.substring(0, className.length()-6).replace('/', '.');
                        classList.add(className);
                    }
                }
            }
        }

        private String asPath(Annotation a) {
            if (a != null && a.annotationType().equals(javax.ws.rs.Path.class)) {
                String val = ((javax.ws.rs.Path) a).value();
                if (val == null) {
                    val = "";
                }
                return val;
            }
            return null;
        }

        private String [] asConsumes(Annotation a) {
            if (a != null && a.annotationType().equals(javax.ws.rs.Consumes.class)) {
                String [] val = ((javax.ws.rs.Consumes) a).value();
                return val;
            }
            return null;
        }

        private String [] asProduces(Annotation a) {
            if (a != null && a.annotationType().equals(javax.ws.rs.Produces.class)) {
                String [] val = ((javax.ws.rs.Produces) a).value();
                return val;
            }
            return null;
        }

        private String asHttpVerb(Annotation a) {
            if (a != null && endpointVerbs.contains(a.annotationType())) {
                return a.annotationType().getSimpleName();
            }
            return null;
        }

        private List<EndpointInfo> endpointsForPath(String fullPath) {
            List<EndpointInfo> endpts = endpoints.get(fullPath);
            if (endpts == null) {
                endpts = new LinkedList<>();
                endpoints.put(fullPath, endpts);
            }
            return endpts;
        }
    }

    static class ConfigInfo {
        /**
         * Locally declared url path segment for the endpoint.
         * Effective url path takes the hierarchy of parents into account.
         * If method name is not null, then path has to be non-null
         */
        String path;

        /**
         * Class that defines this config info
         */
        Class clazz;

        /**
         * Parent config info
         */
        ConfigInfo parent;

        /**
         * Endpoints defined by the class of this ConfigInfo, indexed by relative path
         */
        LinkedList<EndpointInfo> endpoints = new LinkedList<>();
        private String [] consumes;
        private String [] produces;

        ConfigInfo(Class clazz) {
            this(clazz, null, null);
        }

        ConfigInfo(Class clazz, String path) {
            this(clazz, path, null);
        }

        ConfigInfo(Class clazz, String path, ConfigInfo parent) {
            this.clazz = clazz;
            this.path = path;
            this.parent = parent;
        }

        public ConfigInfo() {

        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public Class getClazz() {
            return clazz;
        }

        public void setClazz(Class clazz) {
            this.clazz = clazz;
        }

        public ConfigInfo getParent() {
            return parent;
        }

        public void setParent(ConfigInfo parent) {
            this.parent = parent;
        }

        public List<EndpointInfo> getEndpoints() {
            return endpoints;
        }

        public String [] getConsumes() {
            return consumes;
        }

        public void setConsumes(String [] consumes) {
            this.consumes = consumes;
        }

        public String [] getProduces() {
            return produces;
        }

        public void setProduces(String [] produces) {
            this.produces = produces;
        }
    }


    static class EndpointInfo {

        /**
         * Locally declared url path segment for the endpoint.
         * Effective url path takes the hierarchy of parents into account.
         * If method name is not null, then path has to be non-null
         */
        String path;

        /**
         * Config info of the class on which this endpoint is defined
         */
        ConfigInfo parent;


        /**
         * Implementation method info
         */
        Method method;

        /**
         * Verb used for method
         */
        String verb;

        /**
         * Mime types
         */
        String [] consumes;

        /**
         * Mime types
         */
        String [] produces;

        /**
         * Query param names
         */
        private List<String> queryParams = new LinkedList<>();

        /**
         * Body param types
         */
        private List<Class> bodyParams = new LinkedList<>();

        /**
         * Impl method name
         */
        private String methodName;

        public EndpointInfo(ConfigInfo parent, Method m, String verb, String path, String[] consumes, String[] produces, List<String> namedParams, List<Class> bodyParams) {
            this.parent = parent;
            this.method = m;
            this.verb = verb;
            this.path = path;
            this.consumes = consumes;
            this.produces = produces;
            this.queryParams = namedParams;
            this.bodyParams = bodyParams;
        }

        public String getFullPath() {
            LinkedList<String> parents = new LinkedList<>();
            if (path != null) {
                parents.add(path);
            }
            ConfigInfo p = parent;
            while (p != null) {
                parents.add(p.getPath());
                p = p.getParent();
            }
            StringBuilder fpath = new StringBuilder();
            ListIterator<String> it = parents.listIterator(parents.size());
            while (it.hasPrevious()) {
                String segment = it.previous();
                if (segment != null && segment.length() > 0) {
                    if (segment.charAt(0) != '/') {
                        fpath.append("/");
                    }
                    fpath.append(segment);
                }
            }
            return fpath.toString();
        }

        public Set<String> getConsumes() {
            // work up the parent hierarchy
            if (consumes != null) {
                return new HashSet(Arrays.asList(consumes));
            }
            HashSet<String> ret = new HashSet<>();
            ConfigInfo p = parent;
            while (p != null) {
                if (p.getConsumes() != null) {
                    for (String c: p.getConsumes()) {
                        ret.add(c);
                    }
                }
                p = p.getParent();
            }
            if (ret.size() == 0) {
                // that's a default it appears
                ret.add("application/json");
            }
            return ret;
        }

        public Set<String> getProduces() {
            // work up the parent hierarchy
            if (produces != null) {
                return new HashSet(Arrays.asList(produces));
            }
            HashSet<String> ret = new HashSet<>();
            ConfigInfo p = parent;
            while (p != null) {
                if (p.getProduces() != null) {
                    for (String c: p.getProduces()) {
                        ret.add(c);
                    }
                }
                p = p.getParent();
            }
            if (ret.size() == 0) {
                // that's a default it appears
                ret.add("application/json");
            }
            return ret;
        }

        public String getVerb() {
            return verb;
        }

        public Class getReturnType() {
            return method.getReturnType();
        }

        public List<String> getQueryParams() {
            return queryParams;
        }

        public List<Class> getBodyParams() {
            return bodyParams;
        }

        public String getMethodName() {
            return methodName;
        }

        public Method getMethod() {
            return method;
        }
    }
}
