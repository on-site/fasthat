package com.on_site.fasthat.ant;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.MacroDef;
import org.apache.tools.ant.taskdefs.MacroInstance;
import org.apache.tools.ant.types.XMLCatalog;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class EachPomDependencyTask extends Task {
    private MacroDef macroDef;

    @Override
    public void execute() throws BuildException {
        addAttribute("dep-group-id");
        addAttribute("dep-artifact-id");
        addAttribute("dep-version");
        addAttribute("dep-jar");
        addAttribute("dep-maven-url");
        parsePom().forEach(this::iterate);
    }

    public Object createSequential() {
        macroDef = new MacroDef();
        macroDef.setProject(getProject());
        return macroDef.createSequential();
    }

    private void addAttribute(String name) {
        MacroDef.Attribute attribute = new MacroDef.Attribute();
        attribute.setName(name);
        macroDef.addConfiguredAttribute(attribute);
    }

    private void iterate(Dependency dependency) {
        MacroInstance instance = new MacroInstance();
        instance.setProject(getProject());
        instance.setOwningTarget(getOwningTarget());
        instance.setMacroDef(macroDef);
        instance.setDynamicAttribute("dep-group-id", dependency.getGroupId());
        instance.setDynamicAttribute("dep-artifact-id", dependency.getArtifactId());
        instance.setDynamicAttribute("dep-version", dependency.getVersion());
        instance.setDynamicAttribute("dep-jar", dependency.getJar());
        instance.setDynamicAttribute("dep-maven-url", dependency.getMavenUrl());
        instance.execute();
    }

    private List<Dependency> parsePom() throws BuildException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setValidating(false);
            factory.setNamespaceAware(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            XMLCatalog catalog = new XMLCatalog();
            catalog.setProject(getProject());
            builder.setEntityResolver(catalog);
            File pomFile = new File(getProject().getBaseDir(), "pom.xml");
            Document document = builder.parse(pomFile);
            Element root = document.getDocumentElement();
            List<Dependency> result = new ArrayList<>();
            XPath xpath = XPathFactory.newInstance().newXPath();
            NodeList nodes = (NodeList) xpath.evaluate("/project/dependencies/dependency", root, XPathConstants.NODESET);

            for (int i = 0; i < nodes.getLength(); i++) {
                Dependency dependency = new Dependency(xpath, (Element) nodes.item(i));

                if (!dependency.isTestScope()) {
                    result.add(dependency);
                }
            }

            return result;
        } catch (BuildException e) {
            throw e;
        } catch (Exception e) {
            throw new BuildException("Error parsing pom.xml", e);
        }
    }

    private static class Dependency {
        private final String groupId;
        private final String artifactId;
        private final String version;
        private final String scope;

        public Dependency(XPath xpath, Element element) throws BuildException {
            this.groupId = getNodeText(xpath, "groupId", element);
            this.artifactId = getNodeText(xpath, "artifactId", element);
            this.version = getNodeText(xpath, "version", element);
            this.scope = getOptionalNodeText(xpath, "scope", element);
        }

        public boolean isTestScope() {
            return "test".equals(scope);
        }

        private String getOptionalNodeText(XPath xpath, String query, Element element) throws BuildException {
            Element node = getNode(xpath, query, element);

            if (node == null) {
                return null;
            }

            return node.getTextContent().trim();
        }

        private String getNodeText(XPath xpath, String query, Element element) throws BuildException {
            return getNode(xpath, query, element).getTextContent().trim();
        }

        private Element getNode(XPath xpath, String query, Element element) throws BuildException {
            try {
                return (Element) xpath.evaluate(query, element, XPathConstants.NODE);
            } catch (Exception e) {
                throw new BuildException("Error running XPath query: " + query, e);
            }
        }

        public String getGroupId() {
            return groupId;
        }

        public String getArtifactId() {
            return artifactId;
        }

        public String getVersion() {
            return version;
        }

        public String getJar() {
            return getArtifactId() + "-" + getVersion() + ".jar";
        }

        public String getMavenUrl() {
            return "http://central.maven.org/maven2/" + getGroupId().replace('.', '/') + "/" + getArtifactId() + "/" + getVersion() + "/" + getJar();
        }
    }
}
