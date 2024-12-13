node{
     
    stage('SCM Checkout'){
        git url: 'https://github.com/MithunTechnologiesDevOps/java-web-app-docker.git',branch: 'master'
    }
    
    stage(" Maven Clean Package"){
      def mavenHome =  tool name: "Maven-3.5.6", type: "maven"
      def mavenCMD = "${mavenHome}/bin/mvn"
      sh "${mavenCMD} clean package"
      
    } 
    
    
    stage('Build Docker Image'){
        sh 'docker build -t dockerhandson/java-web-app .'
    }
    
    stage('Push Docker Image'){
        withCredentials([string(credentialsId: 'Docker_Hub_Pwd', variable: 'Docker_Hub_Pwd')]) {
          sh "docker login -u dockerhandson -p ${Docker_Hub_Pwd}"
        }
        sh 'docker push dockerhandson/java-web-app'
     }
     
      stage('Run Docker Image In Dev Server'){
        
        def dockerRun = ' docker run  -d -p 8080:8080 --name java-web-app dockerhandson/java-web-app'
         
         sshagent(['DOCKER_SERVER']) {
          sh 'ssh -o StrictHostKeyChecking=no ubuntu@172.31.20.72 docker stop java-web-app || true'
          sh 'ssh  ubuntu@172.31.20.72 docker rm java-web-app || true'
          sh 'ssh  ubuntu@172.31.20.72 docker rmi -f  $(docker images -q) || true'
          sh "ssh  ubuntu@172.31.20.72 ${dockerRun}"
       }
       
    }
     
     
}
package com.rst.helloworld.web;

import java.util.Map;

import com.rst.helloworld.service.HelloWorldService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class WelcomeController {

private final Logger logger = LoggerFactory.getLogger(WelcomeController.class);
private final HelloWorldService helloWorldService;

@Autowired
public WelcomeController(HelloWorldService helloWorldService) {
this.helloWorldService = helloWorldService;
}

@RequestMapping(value = "/", method = RequestMethod.GET)
public String index(Map<String, Object> model) {

logger.debug("index() is executed!");

model.put("title", helloWorldService.getTitle(""));
model.put("msg", helloWorldService.getDesc());

return "index";
}

@RequestMapping(value = "/hello/{name:.+}", method = RequestMethod.GET)
public ModelAndView hello(@PathVariable("name") String name) {

logger.debug("hello() is executed - $name {}", name);

ModelAndView model = new ModelAndView();
model.setViewName("index");

model.addObject("title", helloWorldService.getTitle(name));
model.addObject("msg", helloWorldService.getDesc());

return model;

}

}---
apiVersion: v1
kind: ReplicationController
metadata:
  labels:
    name: javawebapp
  name: java-controller
spec:
  replicas: 1
  template:
    metadata:
      labels:
        name: javawebapp
    spec:
      containers:
      - image: dockerhandson/java-web-app
        name: javawebapp
        ports:
        - name: javawebapp
          containerPort: 8080  
---
# Node Port Service
apiVersion: v1
kind: Service
metadata:
  labels:
    name: javawebapp
  name: javawebapp
spec:
  type: NodePort
  ports:
    - port: 8080
      targetPort: 8080
  selector:
    name: javawebapp
