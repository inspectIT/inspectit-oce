import React, { useState, useEffect } from 'react';
import useFetchData from '../../../../hooks/use-fetch-data';
import { Dialog } from 'primereact/dialog';
import { InputText } from 'primereact/inputtext';
import { ToggleButton } from 'primereact/togglebutton';
import { Button } from 'primereact/button';
import { ListBox } from 'primereact/listbox';
import _ from 'lodash';

const SearchResultTemplate = ({ filename, lineNumber, line, matches }) => {
  // split matches
  let index = 0;
  const split = [];
  _.each(matches, (match) => {
    const { start, end } = match;
    if (start > index) {
      const previousText = line.substring(index, start);
      split.push({ text: previousText, highlight: false });
    }
    const matchText = line.substring(start, end);
    split.push({ text: matchText, highlight: true });
    index = end;
  });

  if (index < line.length) {
    const endingText = line.substring(index);
    split.push({ text: endingText, highlight: false });
  }

  split[0].text = split[0].text.replace(/^\s+/, ''); // trim leading spaces

  return (
    <>
      <style jsx>{`
        .item {
          display: flex;
        }
        .match {
          flex-grow: 1;
          font-family: monospace;
          overflow: hidden;
          white-space: nowrap;
          text-overflow: ellipsis;
        }
        .source {
          color: #9e9e9e;
          margin-left: 2rem;
          white-space: nowrap;
        }
        .source span {
          color: black;
        }
        :global(.p-highlight) .source,
        :global(.p-highlight) .source span {
          color: inherit;
        }
        .highlight {
          background-color: #ffa726;
          padding: 0 0.25rem;
          border-radius: 4px;
        }
      `}</style>

      <div className="item">
        <div className="match">
          {split.map((match, index) =>
            match.highlight ? (
              <span key={index} className="highlight">
                {match.text}
              </span>
            ) : (
              match.text
            )
          )}
        </div>
        <div className="source">
          {filename} <span>{lineNumber + 1}</span>
        </div>
      </div>
    </>
  );
};

const SearchDialog = () => {
  // state variables
  const [searchTarget, setSearchTarget] = useState(0); // the current selected file name
  const [resultSelection, setResultSelection] = useState(null);
  const [searchResults, setSearchResults] = useState([]);
  const [query, setQuery] = useState('');

  const [{ data, isLoading, lastUpdate }, refreshData] = useFetchData('/search', { query: query, 'include-first-line': true });

  const executeSearch = () => {
    if (query) {
      refreshData();
    }
  };

  useEffect(() => {
    console.log('eff');
    const result = _(data)
      .groupBy((element) => element.file + ':' + element.startLine)
      .map((elements, key) => {
        const matches = _.map(elements, (element) => {
          return {
            start: element.startColumn,
            end: element.endColumn,
          };
        });

        return {
          key,
          filename: elements[0].file, // always the same because we group for this
          lineNumber: elements[0].startLine, // always the same because we group for this
          line: elements[0].firstLine, // must be the same for all elements
          matches,
        };
      })

      .value();
    console.log(result);

    setSearchResults(result);
  }, [data]);

  const footer = (
    <div>
      <Button label="Open File" icon="pi pi-external-link" /*onClick={this.onHide}*/ />
    </div>
  );

  return (
    <>
      <style jsx>{`
        .this :global(.p-dialog-content) {
          padding: 0;
          display: flex;
          flex-direction: column;
          align-items: stretch;
          height: 75vh;
        }
        .input-container {
          margin: 0.5rem;
        }
        .target :global(.p-togglebutton) {
          margin-right: 0.5rem;
        }
        .query {
          margin-bottom: 0.5rem;
        }
        .query :global(.query-input) {
          flex-grow: 1;
        }
        .this :global(.p-listbox) {
          width: 100%;
          border: 0;
          border-radius: 0;
          overflow-y: auto;
        }
      `}</style>

      <div className="this">
        <Dialog
          className="search-dialog"
          header="Search in Configuration Files"
          visible={true}
          style={{ width: '50vw', minWidth: '50rem' }}
          //onHide={() => this.onHide('displayBlockScroll')}
          blockScroll
          footer={footer}
        >
          <div className="input-container">
            <div className="query p-inputgroup">
              <span className="p-inputgroup-addon">
                <i className="pi pi-search"></i>
              </span>
              <InputText
                className="query-input"
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                onKeyPress={(e) => e.key === 'Enter' && executeSearch()}
              />
              <Button icon="pi pi-search" onClick={executeSearch} disabled={!query} />
            </div>

            <div className="target">
              <ToggleButton
                onLabel="In Workspace"
                offLabel="In Workspace"
                checked={searchTarget == 0}
                onChange={() => setSearchTarget(0)}
              />
              <ToggleButton
                onLabel="In Directory"
                offLabel="In Directory"
                checked={searchTarget == 1}
                onChange={() => setSearchTarget(1)}
                disabled={true}
              />
              <ToggleButton
                onLabel="In Agent Mapping"
                offLabel="In Agent Mapping"
                checked={searchTarget == 2}
                onChange={() => setSearchTarget(2)}
                disabled={true}
              />
            </div>
          </div>

          <ListBox
            optionLabel="key"
            value={resultSelection}
            options={searchResults}
            onChange={(e) => setResultSelection(e.value)}
            itemTemplate={SearchResultTemplate}
          />
        </Dialog>
      </div>
    </>
  );
};

export default SearchDialog;
