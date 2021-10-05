package org.batfish.common;

import static com.google.common.base.MoreObjects.firstNonNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.annotations.VisibleForTesting;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format.Builtin;
import io.opentracing.propagation.TextMapAdapter;
import io.opentracing.util.GlobalTracer;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;

public class WorkItem {
  private static final String PROP_NETWORK = "containerName";
  private static final String PROP_ID = "id";
  private static final String PROP_REQUEST_PARAMS = "requestParams";
  private static final String PROP_SNAPSHOT = "testrigName";

  // used for testing to force an UUID
  private static UUID FIXED_UUID = null;

  private final String _network;
  private final UUID _id;
  private Map<String, String> _requestParams;
  private final String _snapshot;
  private Map<String, String> _spanData; /* Map used by the TextMap carrier for SpanContext */

  public WorkItem(String containerName, String testrigName) {
    this(
        (FIXED_UUID == null) ? UUID.randomUUID() : FIXED_UUID,
        containerName,
        testrigName,
        new HashMap<>());
  }

  @JsonCreator
  public WorkItem(
      @JsonProperty(PROP_ID) UUID id,
      @JsonProperty(PROP_NETWORK) String network,
      @JsonProperty(PROP_SNAPSHOT) String snapshot,
      @JsonProperty(PROP_REQUEST_PARAMS) Map<String, String> reqParams) {
    _id = id;
    _network = network;
    _snapshot = snapshot;
    _requestParams = firstNonNull(reqParams, new HashMap<>());
    _spanData = new HashMap<>();
  }

  public static WorkItem getWorkItemAnswerQuestion(
      String questionName,
      String containerName,
      String testrigName,
      String deltaTestrig,
      boolean isDifferential) {
    return getWorkItemAnswerQuestion(
        questionName, containerName, testrigName, deltaTestrig, null, isDifferential);
  }

  public static WorkItem getWorkItemAnswerQuestion(
      String questionName,
      String containerName,
      String testrigName,
      String deltaTestrig,
      String analysisName,
      boolean isDifferential) {
    WorkItem wItem = new WorkItem(containerName, testrigName);
    wItem.addRequestParam(BfConsts.COMMAND_ANSWER, "");
    wItem.addRequestParam(BfConsts.ARG_QUESTION_NAME, questionName);
    if (isDifferential) {
      wItem.addRequestParam(BfConsts.ARG_DIFFERENTIAL, "");
    }
    if (deltaTestrig != null) {
      wItem.addRequestParam(BfConsts.ARG_DELTA_TESTRIG, deltaTestrig);
    }
    if (analysisName != null) {
      wItem.addRequestParam(BfConsts.ARG_ANALYSIS_NAME, analysisName);
    }
    return wItem;
  }

  public static WorkItem getWorkItemGenerateDataPlane(String network, String snapshot) {
    WorkItem wItem = new WorkItem(network, snapshot);
    wItem.addRequestParam(BfConsts.COMMAND_DUMP_DP, "");
    return wItem;
  }

  public static WorkItem getWorkItemParse(String containerName, String testrigName) {
    WorkItem wItem = new WorkItem(containerName, testrigName);
    wItem.addRequestParam(BfConsts.COMMAND_PARSE_VENDOR_INDEPENDENT, "");
    wItem.addRequestParam(BfConsts.COMMAND_PARSE_VENDOR_SPECIFIC, "");
    wItem.addRequestParam(BfConsts.COMMAND_INIT_INFO, "");
    wItem.addRequestParam(BfConsts.ARG_IGNORE_MANAGEMENT_INTERFACES, "");
    return wItem;
  }

  public static WorkItem getWorkItemRunAnalysis(
      String analysisName, String containerName, String testrigName) {
    WorkItem wItem = new WorkItem(containerName, testrigName);
    wItem.addRequestParam(BfConsts.COMMAND_ANALYZE, "");
    wItem.addRequestParam(BfConsts.ARG_ANALYSIS_NAME, analysisName);
    wItem.addRequestParam(BfConsts.ARG_TESTRIG, testrigName);
    return wItem;
  }

  public static boolean isAnalyzingWorkItem(WorkItem workItem) {
    return workItem.getRequestParams().containsKey(BfConsts.COMMAND_ANALYZE);
  }

  public static boolean isAnsweringWorkItem(WorkItem workItem) {
    return workItem.getRequestParams().containsKey(BfConsts.COMMAND_ANSWER);
  }

  public static boolean isDataplaningWorkItem(WorkItem workItem) {
    return workItem.getRequestParams().containsKey(BfConsts.COMMAND_DUMP_DP);
  }

  public static boolean isDifferential(WorkItem workItem) {
    return workItem.getRequestParams().containsKey(BfConsts.ARG_DIFFERENTIAL);
  }

  public static boolean isParsingWorkItem(WorkItem workItem) {
    return workItem.getRequestParams().containsKey(BfConsts.COMMAND_PARSE_VENDOR_SPECIFIC);
  }

  public static String getAnalysisName(WorkItem workItem) {
    return workItem.getRequestParams().get(BfConsts.ARG_ANALYSIS_NAME);
  }

  public static String getQuestionName(WorkItem workItem) {
    return workItem.getRequestParams().get(BfConsts.ARG_QUESTION_NAME);
  }

  public static String getReferenceSnapshotName(WorkItem workItem) {
    return workItem.getRequestParams().get(BfConsts.ARG_DELTA_TESTRIG);
  }

  public void addRequestParam(String key, String value) {
    _requestParams.put(key, value);
  }

  @JsonProperty(PROP_NETWORK)
  public String getNetwork() {
    return _network;
  }

  @JsonProperty(PROP_ID)
  public UUID getId() {
    return _id;
  }

  @JsonProperty(PROP_REQUEST_PARAMS)
  public Map<String, String> getRequestParams() {
    return _requestParams;
  }

  /**
   * Retrieves a {@link SpanContext} which was serialized earlier in the {@link WorkItem}
   *
   * @return {@link SpanContext} or null if no {@link SpanContext} was serialized in the {@link
   *     WorkItem}
   */
  @Nullable
  @JsonIgnore
  public SpanContext getSourceSpan() {
    return getSourceSpan(GlobalTracer.get());
  }

  @VisibleForTesting
  SpanContext getSourceSpan(Tracer tracer) {
    return tracer.extract(Builtin.TEXT_MAP, new TextMapAdapter(_spanData));
  }

  @JsonProperty(PROP_SNAPSHOT)
  public String getSnapshot() {
    return _snapshot;
  }

  /**
   * The supplied workItem is a match if it has the same network, snapshot, and request parameters
   *
   * @param workItem The workItem that should be matched
   * @return {@link boolean} that indicates whether the supplied workItem is a match
   */
  public boolean matches(WorkItem workItem) {
    return (workItem != null
        && workItem._network.equals(_network)
        && workItem._snapshot.equals(_snapshot)
        && workItem._requestParams.equals(_requestParams));
  }

  public static void setFixedUuid(UUID value) {
    FIXED_UUID = value;
  }

  /**
   * Takes an Active {@link Span} and attaches it to the {@link WorkItem} which can be fetched later
   * using {@link WorkItem#getSourceSpan()}
   */
  public void setSourceSpan(@Nullable Span activeSpan) {
    setSourceSpan(activeSpan, GlobalTracer.get());
  }

  @VisibleForTesting
  void setSourceSpan(@Nullable Span activeSpan, Tracer tracer) {
    if (activeSpan == null) {
      return;
    }
    tracer.inject(activeSpan.context(), Builtin.TEXT_MAP, new TextMapAdapter(_spanData));
  }

  @Override
  public String toString() {
    return String.format("[%s %s %s %s]", _id, _network, _snapshot, _requestParams);
  }
}
