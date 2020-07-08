/*
 * This file is just a dummy artifact, as long as the Alerting REST interface is not availeable!
 *
 * IGNORE this file for code review!
 */

const topics = [
  {
    id: 'Some Topic',
  },
  {
    id: 'great topic',
  },
  {
    id: 'Some E-Mail distribution',
  },
];

const handlers = {
  'Some Topic': [{ id: 'Handler A' }, { id: 'Handler B' }],
};

export const fetchTopics = (onSuccess, onFailed) => {
  const success = true;
  if (success) {
    if (onSuccess) {
      onSuccess(topics);
    }
  } else {
    if (onFailed) {
      onFailed();
    }
  }
};

export const fetchHandlers = (topicName, onSuccess, onFailed) => {
  const success = true;
  if (success) {
    if (onSuccess) {
      onSuccess(topicName in handlers ? handlers[topicName] : []);
    }
  } else {
    if (onFailed) {
      onFailed();
    }
  }
};
