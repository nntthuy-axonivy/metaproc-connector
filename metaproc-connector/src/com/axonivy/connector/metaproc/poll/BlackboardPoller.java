package com.axonivy.connector.metaproc.poll;

import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.databind.node.ObjectNode;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.process.intermediateevent.AbstractProcessIntermediateEventBean;
import ch.ivyteam.ivy.process.intermediateevent.IProcessIntermediateEventBeanRuntime;
import ch.ivyteam.ivy.workflow.query.TaskQuery;

public class BlackboardPoller extends AbstractProcessIntermediateEventBean {

  private static final String CUSTOM_FIELD = "blackboardId";

  public BlackboardPoller() {
    super("Blackboard Poller", "Polls the result on the meta:proc blackboard", null);
  }

  @Override
  public void initialize(IProcessIntermediateEventBeanRuntime runtime, String configuration) {
    super.initialize(runtime, configuration);
    runtime.setPollTimeInterval(2000);
  }

  @Override
  public void poll() {
    var tasks = TaskQuery.create().where().customField().stringField(CUSTOM_FIELD).isNotNull()
            .executor()
            .results();
    for (var task : tasks) {
      var field = task.customFields().stringField(CUSTOM_FIELD);
      var id = field.getOrNull();
      if (isExecuted(id)) {
        field.delete();
        getEventBeanRuntime().fireProcessIntermediateEventEx(id, null, null);
      }
    }
  }

  public static void waitOnExecuted(String blackboardId) {
    while (!isExecuted(blackboardId)) {
      try {
        Thread.sleep(2000);
      } catch (InterruptedException ex) {
        throw new RuntimeException(ex);
      }
    }
  }

  public static boolean isExecuted(String blackboardId) {
    Ivy.log().info("Check blackboard: " + blackboardId);
    var request = Ivy.rest().client("metaproc").path("metarest").path("datagrid").path("blackboard")
            .path("load").path(blackboardId).request();
    try (var response = request.accept(MediaType.APPLICATION_JSON_TYPE).get()) {
      var entity = response.readEntity(ObjectNode.class);
      var status = entity.get("Blackboard").get("blackboardStatus").asText();
      Ivy.log().info("blackboard entry: " + blackboardId + " status: " + status);
      return "EXECUTED".equals(status);
    }
  }
}
